package backend.xxx.chat.storage.controller;

import backend.xxx.chat.storage.dto.CreatePresignedUploadRequest;
import backend.xxx.chat.storage.dto.PresignedUploadResponse;
import backend.xxx.chat.storage.service.ObjectStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final ObjectStorageService objectStorageService;

    @PostMapping("/presign")
    public ResponseEntity<PresignedUploadResponse> createPresignedUpload(
            @Valid @RequestBody CreatePresignedUploadRequest request
    ) {
        return ResponseEntity.ok(objectStorageService.createPresignedUpload(request));
    }
}
