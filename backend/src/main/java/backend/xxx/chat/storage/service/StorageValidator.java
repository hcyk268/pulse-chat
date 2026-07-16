package backend.xxx.chat.storage.service;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.web.Translator;
import backend.xxx.chat.common.exception.ForbiddenException;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.storage.model.UploadSession;
import backend.xxx.chat.storage.model.UploadSessionStatus;
import backend.xxx.chat.storage.repository.UploadSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageValidator {

    private final UploadSessionRepository uploadSessionRepository;

    public String normalizeContentType(String contentType) {
        if (contentType == null) {
            throw new ValidationException("storage.content.type.blank");
        }

        String normalized = contentType.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new ValidationException("storage.content.type.blank");
        }

        return normalized;
    }

    public void validateContentType(String contentType, List<String> allowedContentTypes) {
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            return;
        }

        boolean allowed = allowedContentTypes.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .anyMatch(value -> value.equalsIgnoreCase(contentType));

        if (!allowed) {
            throw new ValidationException(Translator.toLocale("storage.content.type.not.allowed", contentType));
        }
    }

    public void validateFileSize(Long sizeBytes, long maxFileSizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new ValidationException("storage.size.positive");
        }

        if (sizeBytes > maxFileSizeBytes) {
            throw new ValidationException(Translator.toLocale("storage.size.max.exceeded", maxFileSizeBytes));
        }
    }

    public String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String normalizeEtag(String etag) {
        String normalized = normalizeOptional(etag);
        if (normalized == null) {
            throw new ValidationException("storage.etag.blank");
        }
        return normalized;
    }

    public long normalizeChunkSize(Long requestedChunkSizeBytes, Long fileSizeBytes, long defaultChunkSize, long minChunkSize) {
        long chunkSize = requestedChunkSizeBytes == null ? defaultChunkSize : requestedChunkSizeBytes;
        if (chunkSize <= 0) {
            throw new ValidationException("storage.chunk.size.positive");
        }
        if (fileSizeBytes > minChunkSize && chunkSize < minChunkSize) {
            throw new ValidationException(Translator.toLocale("storage.chunk.size.min", minChunkSize));
        }
        return Math.min(chunkSize, fileSizeBytes);
    }

    public UploadSession requireOwnedSession(Long sessionId, Long userId) {
        if (sessionId == null) {
            throw new ValidationException("storage.session.id.required");
        }
        UploadSession session = uploadSessionRepository.findByIdWithOwner(sessionId)
                .orElseThrow(() -> new NotFoundException("storage.session.not.found"));
        requireOwner(session, userId);
        return session;
    }

    public UploadSession requireOwnedSessionForUpdate(Long sessionId, Long userId) {
        if (sessionId == null) {
            throw new ValidationException("storage.session.id.required");
        }
        UploadSession session = uploadSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException("storage.session.not.found"));
        requireOwner(session, userId);
        return session;
    }

    public void requireOwner(UploadSession session, Long userId) {
        if (!session.belongsTo(userId)) {
            throw new ForbiddenException("storage.session.forbidden");
        }
    }

    public void validateSessionUsable(UploadSession session) {
        if (session.isExpired(Instant.now())) {
            throw new ConflictException("storage.session.expired");
        }

        if (session.getStatus() == UploadSessionStatus.CANCELLED
                || session.getStatus() == UploadSessionStatus.FAILED
                || session.getStatus() == UploadSessionStatus.ATTACHED) {
            throw new ConflictException("storage.session.not.usable");
        }
    }

    public void validatePartNumber(UploadSession session, Integer partNumber) {
        if (partNumber == null || partNumber < 1 || partNumber > session.getTotalParts()) {
            throw new ValidationException("storage.part.number.invalid");
        }
    }

    public void validatePartSize(UploadSession session, Integer partNumber, Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new ValidationException("storage.size.positive");
        }

        boolean lastPart = partNumber.equals(session.getTotalParts());
        long expectedSize = lastPart
                ? session.getSizeBytes() - session.getChunkSizeBytes() * (session.getTotalParts() - 1L)
                : session.getChunkSizeBytes();

        if (sizeBytes != expectedSize) {
            throw new ValidationException("storage.part.size.mismatch");
        }
    }
}
