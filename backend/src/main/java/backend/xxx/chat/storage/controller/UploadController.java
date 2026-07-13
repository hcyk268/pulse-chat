package backend.xxx.chat.storage.controller;

import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.storage.dto.CompleteUploadPartRequest;
import backend.xxx.chat.storage.dto.CreateMultipartUploadRequest;
import backend.xxx.chat.storage.dto.CreatePresignedUploadRequest;
import backend.xxx.chat.storage.dto.MultipartUploadResumeResponse;
import backend.xxx.chat.storage.dto.MultipartUploadSessionResponse;
import backend.xxx.chat.storage.dto.PresignedUploadPartResponse;
import backend.xxx.chat.storage.dto.PresignedUploadResponse;
import backend.xxx.chat.storage.dto.UploadedAssetResponse;
import backend.xxx.chat.storage.service.MultipartUploadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<MultipartUploadSessionResponse> createMultipartUpload(
            @Valid @RequestBody CreateMultipartUploadRequest request
    ) {
        return ResponseEntity.ok(multipartUploadService.createSession(
                currentUserProvider.getCurrentUsername(),
                request
        ));
    }

    @PostMapping("/multipart/{sessionId}/parts/{partNumber}/presign")
    public ResponseEntity<PresignedUploadPartResponse> presignMultipartUploadPart(
            @PathVariable @Positive Long sessionId,
            @PathVariable @Positive Integer partNumber
    ) {
        return ResponseEntity.ok(multipartUploadService.presignPart(
                currentUserProvider.getCurrentUsername(),
                sessionId,
                partNumber
        ));
    }

    @PostMapping("/multipart/{sessionId}/parts/{partNumber}/complete")
    public ResponseEntity<MultipartUploadResumeResponse> completeMultipartUploadPart(
            @PathVariable @Positive Long sessionId,
            @PathVariable @Positive Integer partNumber,
            @Valid @RequestBody CompleteUploadPartRequest request
    ) {
        return ResponseEntity.ok(multipartUploadService.completePart(
                currentUserProvider.getCurrentUsername(),
                sessionId,
                partNumber,
                request
        ));
    }

    @GetMapping("/multipart/{sessionId}/resume")
    public ResponseEntity<MultipartUploadResumeResponse> resumeMultipartUpload(
            @PathVariable @Positive Long sessionId
    ) {
        return ResponseEntity.ok(multipartUploadService.resume(
                currentUserProvider.getCurrentUsername(),
                sessionId
        ));
    }

    @PostMapping("/multipart/{sessionId}/complete")
    public ResponseEntity<UploadedAssetResponse> completeMultipartUpload(
            @PathVariable @Positive Long sessionId
    ) {
        return ResponseEntity.ok(multipartUploadService.complete(
                currentUserProvider.getCurrentUsername(),
                sessionId
        ));
    }

    @PostMapping("/multipart/{sessionId}/abort")
    public ResponseEntity<Void> abortMultipartUpload(
            @PathVariable @Positive Long sessionId
    ) {
        multipartUploadService.abort(currentUserProvider.getCurrentUsername(), sessionId);
        return ResponseEntity.noContent().build();
    }
}