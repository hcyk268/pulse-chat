package backend.xxx.chat.storage.service;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.storage.dto.CreatePresignedUploadRequest;
import backend.xxx.chat.storage.dto.PresignedUploadResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledObjectStorageService implements ObjectStorageService {

    @Override
    public PresignedUploadResponse createPresignedUpload(CreatePresignedUploadRequest request) {
        throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.SERVICE_UNAVAILABLE,
                "Object storage is not configured"
        );
    }
}
