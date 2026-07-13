package backend.xxx.chat.storage.service;

import backend.xxx.chat.common.exception.ServiceUnavailableException;
import backend.xxx.chat.storage.dto.CompleteUploadPartRequest;
import backend.xxx.chat.storage.dto.CreateMultipartUploadRequest;
import backend.xxx.chat.storage.dto.MultipartUploadResumeResponse;
import backend.xxx.chat.storage.dto.MultipartUploadSessionResponse;
import backend.xxx.chat.storage.dto.PresignedUploadPartResponse;
import backend.xxx.chat.storage.dto.UploadedAssetResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledMultipartUploadService implements MultipartUploadService {

    @Override
    public MultipartUploadSessionResponse createSession(String currentUsername, CreateMultipartUploadRequest request) {
        throw unavailable();
    }

    @Override
    public PresignedUploadPartResponse presignPart(String currentUsername, Long sessionId, Integer partNumber) {
        throw unavailable();
    }

    @Override
    public MultipartUploadResumeResponse resume(String currentUsername, Long sessionId) {
        throw unavailable();
    }

    @Override
    public MultipartUploadResumeResponse completePart(String currentUsername, Long sessionId, Integer partNumber, CompleteUploadPartRequest request) {
        throw unavailable();
    }

    @Override
    public UploadedAssetResponse complete(String currentUsername, Long sessionId) {
        throw unavailable();
    }

    @Override
    public void abort(String currentUsername, Long sessionId) {
        throw unavailable();
    }

    private ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException("Object storage is not configured");
    }
}