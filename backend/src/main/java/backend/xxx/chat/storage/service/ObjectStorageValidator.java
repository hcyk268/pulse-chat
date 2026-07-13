package backend.xxx.chat.storage.service;

import java.util.List;

import backend.xxx.chat.common.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class ObjectStorageValidator {

    public String normalizeContentType(String contentType) {
        if (contentType == null) {
            throw new ValidationException("contentType must not be blank");
        }

        String normalized = contentType.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new ValidationException("contentType must not be blank");
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
            throw new ValidationException("contentType is not allowed: " + contentType);
        }
    }

    public void validateFileSize(Long sizeBytes, long maxFileSizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new ValidationException("sizeBytes must be greater than 0");
        }

        if (sizeBytes > maxFileSizeBytes) {
            throw new ValidationException("sizeBytes exceeds max allowed size " + maxFileSizeBytes);
        }
    }
}
