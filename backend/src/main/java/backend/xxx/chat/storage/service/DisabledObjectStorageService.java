package backend.xxx.chat.storage.service;

import backend.xxx.chat.common.exception.ServiceUnavailableException;

import backend.xxx.chat.storage.dto.CreatePresignedUploadRequest;
import backend.xxx.chat.storage.dto.PresignedUploadResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledObjectStorageService implements ObjectStorageService {

    @Override
    public PresignedUploadResponse createPresignedUpload(CreatePresignedUploadRequest request) {
        throw new ServiceUnavailableException("Object storage is not configured");
    }
}
