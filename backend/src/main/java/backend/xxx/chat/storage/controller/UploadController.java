package backend.xxx.chat.storage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Uploads", description = "Multipart upload APIs")
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Validated
public class UploadController {

    private final MultipartUploadService multipartUploadService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "Create multipart upload")
    @PostMapping("/multipart")
    public ResponseData<MultipartUploadSessionResponse> createMultipartUpload(
            @Valid @RequestBody CreateMultipartUploadRequest request
    ) {
        return new ResponseData<>(true, "upload.multipart.create.success", multipartUploadService.createSession(
                currentUserProvider.getCurrentUsername(),
                request
        ));
    }

    @Operation(summary = "Presign upload part")
    @PostMapping("/multipart/{sessionId}/parts/{partNumber}/presign")
    public ResponseData<PresignedUploadPartResponse> presignMultipartUploadPart(
            @PathVariable @Positive Long sessionId,
            @PathVariable @Positive Integer partNumber
    ) {
        return new ResponseData<>(true, "upload.multipart.part.presign.success", multipartUploadService.presignPart(
                currentUserProvider.getCurrentUsername(),
                sessionId,
                partNumber
        ));
    }

    @Operation(summary = "Complete upload part")
    @PostMapping("/multipart/{sessionId}/parts/{partNumber}/complete")
    public ResponseData<MultipartUploadResumeResponse> completeMultipartUploadPart(
            @PathVariable @Positive Long sessionId,
            @PathVariable @Positive Integer partNumber,
            @Valid @RequestBody CompleteUploadPartRequest request
    ) {
        return new ResponseData<>(true, "upload.multipart.part.complete.success", multipartUploadService.completePart(
                currentUserProvider.getCurrentUsername(),
                sessionId,
                partNumber,
                request
        ));
    }

    @Operation(summary = "Resume multipart upload")
    @GetMapping("/multipart/{sessionId}/resume")
    public ResponseData<MultipartUploadResumeResponse> resumeMultipartUpload(
            @PathVariable @Positive Long sessionId
    ) {
        return new ResponseData<>(true, "upload.multipart.resume.success", multipartUploadService.resume(
                currentUserProvider.getCurrentUsername(),
                sessionId
        ));
    }

    @Operation(summary = "Complete multipart upload")
    @PostMapping("/multipart/{sessionId}/complete")
    public ResponseData<UploadedAssetResponse> completeMultipartUpload(
            @PathVariable @Positive Long sessionId
    ) {
        return new ResponseData<>(true, "upload.multipart.complete.success", multipartUploadService.complete(
                currentUserProvider.getCurrentUsername(),
                sessionId
        ));
    }

    @Operation(summary = "Abort multipart upload")
    @PostMapping("/multipart/{sessionId}/abort")
    public ResponseData<Void> abortMultipartUpload(
            @PathVariable @Positive Long sessionId
    ) {
        multipartUploadService.abort(currentUserProvider.getCurrentUsername(), sessionId);
        return new ResponseData<>(true, "upload.multipart.abort.success");
    }
}
