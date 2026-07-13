package backend.xxx.chat.storage.service;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.ForbiddenException;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.config.CloudflareR2Properties;
import backend.xxx.chat.storage.dto.CompleteUploadPartRequest;
import backend.xxx.chat.storage.dto.CreateMultipartUploadRequest;
import backend.xxx.chat.storage.dto.MultipartUploadResumeResponse;
import backend.xxx.chat.storage.dto.MultipartUploadSessionResponse;
import backend.xxx.chat.storage.dto.PresignedUploadPartResponse;
import backend.xxx.chat.storage.dto.UploadedAssetResponse;
import backend.xxx.chat.storage.model.UploadPart;
import backend.xxx.chat.storage.model.UploadSession;
import backend.xxx.chat.storage.model.UploadSessionStatus;
import backend.xxx.chat.storage.model.UploadedAsset;
import backend.xxx.chat.storage.repository.UploadPartRepository;
import backend.xxx.chat.storage.repository.UploadSessionRepository;
import backend.xxx.chat.storage.repository.UploadedAssetRepository;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true")
public class CloudflareR2MultipartUploadService implements MultipartUploadService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final long DEFAULT_CHUNK_SIZE_BYTES = 8L * 1024L * 1024L;
    private static final long MIN_MULTIPART_CHUNK_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_PARTS = 10_000;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final CloudflareR2Properties properties;
    private final StorageValidator storageValidator;
    private final UserLookupService userLookupService;
    private final UploadSessionRepository uploadSessionRepository;
    private final UploadPartRepository uploadPartRepository;
    private final UploadedAssetRepository uploadedAssetRepository;
    private final UploadedAssetMapper uploadedAssetMapper;

    @Override
    @Transactional
    public MultipartUploadSessionResponse createSession(String currentUsername, CreateMultipartUploadRequest request) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        String normalizedContentType = storageValidator.normalizeContentType(request.contentType());
        storageValidator.validateContentType(normalizedContentType, properties.allowedContentTypes());
        storageValidator.validateFileSize(request.sizeBytes(), properties.maxFileSizeBytes());

        long chunkSizeBytes = storageValidator.normalizeChunkSize(request.chunkSizeBytes(), request.sizeBytes(), DEFAULT_CHUNK_SIZE_BYTES, MIN_MULTIPART_CHUNK_SIZE_BYTES);
        int totalParts = calculateTotalParts(request.sizeBytes(), chunkSizeBytes);
        String objectKey = buildObjectKey(request.purpose().keyPrefix(), request.fileName());

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(
                software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest.builder()
                        .bucket(properties.bucket())
                        .key(objectKey)
                        .contentType(normalizedContentType)
                        .build()
        );

        UploadSession session = uploadSessionRepository.save(UploadSession.create(
                currentUser,
                request.purpose(),
                sanitizeFileName(request.fileName()),
                normalizedContentType,
                request.sizeBytes(),
                chunkSizeBytes,
                totalParts,
                objectKey,
                response.uploadId(),
                storageValidator.normalizeOptional(request.fileChecksum()),
                Instant.now().plus(properties.presignedUploadTtl())
        ));

        return uploadedAssetMapper.toSessionResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public PresignedUploadPartResponse presignPart(String currentUsername, Long sessionId, Integer partNumber) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        UploadSession session = storageValidator.requireOwnedSession(sessionId, currentUser.getId());
        storageValidator.validateSessionUsable(session);
        storageValidator.validatePartNumber(session, partNumber);

        PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(
                UploadPartPresignRequest.builder()
                        .signatureDuration(properties.presignedUploadTtl())
                        .uploadPartRequest(UploadPartRequest.builder()
                                .bucket(properties.bucket())
                                .key(session.getObjectKey())
                                .uploadId(session.getR2UploadId())
                                .partNumber(partNumber)
                                .build())
                        .build()
        );

        return new PresignedUploadPartResponse(
                session.getId(),
                partNumber,
                presignedRequest.url().toString(),
                "PUT",
                Map.of(),
                Instant.now().plus(properties.presignedUploadTtl())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MultipartUploadResumeResponse resume(String currentUsername, Long sessionId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        UploadSession session = storageValidator.requireOwnedSession(sessionId, currentUser.getId());
        return buildResumeResponse(session);
    }

    @Override
    @Transactional
    public MultipartUploadResumeResponse completePart(
            String currentUsername,
            Long sessionId,
            Integer partNumber,
            CompleteUploadPartRequest request
    ) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        UploadSession session = storageValidator.requireOwnedSessionForUpdate(sessionId, currentUser.getId());
        storageValidator.validateSessionUsable(session);
        storageValidator.validatePartNumber(session, partNumber);
        storageValidator.validatePartSize(session, partNumber, request.sizeBytes());

        UploadPart part = uploadPartRepository.findByUploadSessionIdAndPartNumber(session.getId(), partNumber)
                .orElse(null);
        if (part == null) {
            uploadPartRepository.save(UploadPart.create(
                    session,
                    partNumber,
                    storageValidator.normalizeEtag(request.etag()),
                    request.sizeBytes(),
                    Instant.now()
            ));
        } else {
            part.update(storageValidator.normalizeEtag(request.etag()), request.sizeBytes(), Instant.now());
        }

        session.markUploading();
        return buildResumeResponse(session);
    }

    @Override
    @Transactional
    public UploadedAssetResponse complete(String currentUsername, Long sessionId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        UploadSession session = storageValidator.requireOwnedSessionForUpdate(sessionId, currentUser.getId());
        storageValidator.validateSessionUsable(session);

        UploadedAsset existingAsset = uploadedAssetRepository.findByUploadSessionId(session.getId()).orElse(null);
        if (existingAsset != null) {
            return uploadedAssetMapper.toAssetResponse(existingAsset);
        }

        List<UploadPart> parts = uploadPartRepository.findByUploadSessionIdOrderByPartNumberAsc(session.getId());
        if (parts.size() != session.getTotalParts()) {
            throw new ConflictException("Upload is missing one or more parts");
        }

        List<CompletedPart> completedParts = parts.stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.getPartNumber())
                        .eTag(part.getEtag())
                        .build())
                .toList();

        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(properties.bucket())
                .key(session.getObjectKey())
                .uploadId(session.getR2UploadId())
                .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build())
                .build());

        HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(session.getObjectKey())
                .build());
        if (head.contentLength() != null && !head.contentLength().equals(session.getSizeBytes())) {
            session.markFailed();
            throw new ConflictException("Uploaded object size does not match expected size");
        }

        session.markCompleted(Instant.now());
        session.markVerified();

        UploadedAsset asset = uploadedAssetRepository.save(UploadedAsset.readyFromSession(
                session,
                buildPublicUrl(session.getObjectKey()),
                head.contentLength()
        ));

        return uploadedAssetMapper.toAssetResponse(asset);
    }

    @Override
    @Transactional
    public void abort(String currentUsername, Long sessionId) {
        User currentUser = userLookupService.getCurrentUser(currentUsername);
        UploadSession session = storageValidator.requireOwnedSessionForUpdate(sessionId, currentUser.getId());

        if (session.getStatus() == UploadSessionStatus.ATTACHED || session.getStatus() == UploadSessionStatus.VERIFIED) {
            throw new ConflictException("Completed upload cannot be aborted");
        }

        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(properties.bucket())
                .key(session.getObjectKey())
                .uploadId(session.getR2UploadId())
                .build());
        session.markCancelled();
    }

    private MultipartUploadResumeResponse buildResumeResponse(UploadSession session) {
        List<Integer> uploadedParts = uploadPartRepository.findUploadedPartNumbers(session.getId());
        List<Integer> missingParts = IntStream.range(1, session.getTotalParts() + 1)
                .filter(part -> !uploadedParts.contains(part))
                .boxed()
                .toList();

        return new MultipartUploadResumeResponse(
                session.getId(),
                session.getStatus(),
                session.getTotalParts(),
                session.getChunkSizeBytes(),
                uploadedParts,
                missingParts,
                session.getExpiresAt()
        );
    }

    private int calculateTotalParts(Long fileSizeBytes, long chunkSizeBytes) {
        long totalParts = (fileSizeBytes + chunkSizeBytes - 1) / chunkSizeBytes;
        if (totalParts > MAX_PARTS) {
            throw new ValidationException("file requires too many upload parts");
        }
        return (int) totalParts;
    }

    private String buildObjectKey(String prefix, String originalFileName) {
        String dateFolder = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMATTER);
        return prefix + "/" + dateFolder + "/" + UUID.randomUUID() + "-" + sanitizeFileName(originalFileName);
    }

    private String sanitizeFileName(String fileName) {
        String normalized = fileName == null ? "" : fileName.trim().replace("\\", "/");
        int lastSlashIndex = normalized.lastIndexOf('/');
        String baseName = lastSlashIndex >= 0 ? normalized.substring(lastSlashIndex + 1) : normalized;
        String sanitized = baseName.replaceAll("[^A-Za-z0-9._-]", "-");

        if (sanitized.isBlank()) {
            return "file";
        }

        return sanitized.length() > 120 ? sanitized.substring(sanitized.length() - 120) : sanitized;
    }

    private String buildPublicUrl(String objectKey) {
        String publicBaseUrl = properties.publicBaseUrl();
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return null;
        }

        String normalizedBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;

        return URI.create(normalizedBaseUrl + "/" + objectKey).toString();
    }
}