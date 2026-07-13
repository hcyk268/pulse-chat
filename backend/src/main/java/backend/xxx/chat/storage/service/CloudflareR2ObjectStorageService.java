package backend.xxx.chat.storage.service;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import backend.xxx.chat.config.CloudflareR2Properties;
import backend.xxx.chat.storage.dto.CreatePresignedUploadRequest;
import backend.xxx.chat.storage.dto.PresignedUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true")
public class CloudflareR2ObjectStorageService implements ObjectStorageService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final S3Presigner s3Presigner;
    private final CloudflareR2Properties properties;
    private final ObjectStorageValidator objectStorageValidator;

    @Override
    public PresignedUploadResponse createPresignedUpload(CreatePresignedUploadRequest request) {
        String normalizedContentType = objectStorageValidator.normalizeContentType(request.contentType());
        objectStorageValidator.validateContentType(normalizedContentType, properties.allowedContentTypes());
        objectStorageValidator.validateFileSize(request.sizeBytes(), properties.maxFileSizeBytes());

        String objectKey = buildObjectKey(request.purpose().keyPrefix(), request.fileName());
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(properties.presignedUploadTtl())
                        .putObjectRequest(
                                PutObjectRequest.builder()
                                        .bucket(properties.bucket())
                                        .key(objectKey)
                                        .contentType(normalizedContentType)
                                        .build()
                        )
                        .build()
        );

        Map<String, String> requiredHeaders = new LinkedHashMap<>();
        requiredHeaders.put("Content-Type", normalizedContentType);

        return new PresignedUploadResponse(
                request.purpose(),
                objectKey,
                presignedRequest.url().toString(),
                buildPublicUrl(objectKey),
                "PUT",
                requiredHeaders,
                Instant.now().plus(properties.presignedUploadTtl())
        );
    }

    private String buildObjectKey(String prefix, String originalFileName) {
        String dateFolder = LocalDate.now(ZoneOffset.UTC).format(DATE_FORMATTER);
        String sanitizedFileName = sanitizeFileName(originalFileName);
        return prefix + "/" + dateFolder + "/" + UUID.randomUUID() + "-" + sanitizedFileName;
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