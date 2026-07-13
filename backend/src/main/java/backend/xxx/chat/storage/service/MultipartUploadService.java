package backend.xxx.chat.storage.service;

import backend.xxx.chat.storage.dto.CompleteUploadPartRequest;
import backend.xxx.chat.storage.dto.CreateMultipartUploadRequest;
import backend.xxx.chat.storage.dto.MultipartUploadResumeResponse;
import backend.xxx.chat.storage.dto.MultipartUploadSessionResponse;
import backend.xxx.chat.storage.dto.PresignedUploadPartResponse;
import backend.xxx.chat.storage.dto.UploadedAssetResponse;

public interface MultipartUploadService {

    MultipartUploadSessionResponse createSession(String currentUsername, CreateMultipartUploadRequest request);

    PresignedUploadPartResponse presignPart(String currentUsername, Long sessionId, Integer partNumber);

    MultipartUploadResumeResponse resume(String currentUsername, Long sessionId);

    MultipartUploadResumeResponse completePart(
            String currentUsername,
            Long sessionId,
            Integer partNumber,
            CompleteUploadPartRequest request
    );

    UploadedAssetResponse complete(String currentUsername, Long sessionId);

    void abort(String currentUsername, Long sessionId);
}