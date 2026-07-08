package backend.xxx.chat.storage.service;

import backend.xxx.chat.storage.dto.CreatePresignedUploadRequest;
import backend.xxx.chat.storage.dto.PresignedUploadResponse;

public interface ObjectStorageService {

    PresignedUploadResponse createPresignedUpload(CreatePresignedUploadRequest request);
}
