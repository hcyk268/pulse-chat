package backend.xxx.chat.storage.controller;

import backend.xxx.chat.common.dto.ResponseData;
import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.storage.dto.CompleteUploadPartRequest;
import backend.xxx.chat.storage.dto.CreateMultipartUploadRequest;
import backend.xxx.chat.storage.dto.MultipartUploadResumeResponse;
import backend.xxx.chat.storage.dto.MultipartUploadSessionResponse;
import backend.xxx.chat.storage.dto.PresignedUploadPartResponse;
import backend.xxx.chat.storage.dto.UploadedAssetResponse;
import backend.xxx.chat.storage.service.MultipartUploadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Validated
public class UploadController {

    private final MultipartUploadService multipartUploadService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/multipart")
    public ResponseData<MultipartUploadSessionResponse> createMultipartUpload(
            @Valid @RequestBody CreateMultipartUploadRequest request
    ) {
        return new ResponseData<>(true, "Create multipart upload successfully", multipartUploadService.createSession(
                currentUserProvider.getCurrentUsername(),
                request
        ));
    }

    @PostMapping("/multipart/{sessionId}/parts/{partNumber}/presign")
    public ResponseData<PresignedUploadPartResponse> presignMultipartUploadPart(
            @PathVariable @Positive Long sessionId,
            @PathVariable @Positive Integer partNumber
    ) {
        return new ResponseData<>(true, "Create presigned upload part successfully", multipartUploadService.presignPart(
                currentUserProvider.getCurrentUsername(),
                sessionId,
                partNumber
        ));
    }

    @PostMapping("/multipart/{sessionId}/parts/{partNumber}/complete")
    public ResponseData<MultipartUploadResumeResponse> completeMultipartUploadPart(
            @PathVariable @Positive Long sessionId,
            @PathVariable @Positive Integer partNumber,
            @Valid @RequestBody CompleteUploadPartRequest request
    ) {
        return new ResponseData<>(true, "Complete multipart upload part successfully", multipartUploadService.completePart(
                currentUserProvider.getCurrentUsername(),
                sessionId,
                partNumber,
                request
        ));
    }

    @GetMapping("/multipart/{sessionId}/resume")
    public ResponseData<MultipartUploadResumeResponse> resumeMultipartUpload(
            @PathVariable @Positive Long sessionId
    ) {
        return new ResponseData<>(true, "Resume multipart upload successfully", multipartUploadService.resume(
                currentUserProvider.getCurrentUsername(),
                sessionId
        ));
    }

    @PostMapping("/multipart/{sessionId}/complete")
    public ResponseData<UploadedAssetResponse> completeMultipartUpload(
            @PathVariable @Positive Long sessionId
    ) {
        return new ResponseData<>(true, "Complete multipart upload successfully", multipartUploadService.complete(
                currentUserProvider.getCurrentUsername(),
                sessionId
        ));
    }

    @PostMapping("/multipart/{sessionId}/abort")
    public ResponseData<Void> abortMultipartUpload(
            @PathVariable @Positive Long sessionId
    ) {
        multipartUploadService.abort(currentUserProvider.getCurrentUsername(), sessionId);
        return new ResponseData<>(true, "Abort multipart upload successfully");
    }
}
