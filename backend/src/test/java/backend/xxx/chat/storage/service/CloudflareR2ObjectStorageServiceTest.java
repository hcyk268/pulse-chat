package backend.xxx.chat.storage.service;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.storage.dto.CreatePresignedUploadRequest;
import backend.xxx.chat.storage.dto.PresignedUploadResponse;
import backend.xxx.chat.storage.model.UploadPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.storage.r2.enabled=true",
        "app.storage.r2.account-id=test-account-id",
        "app.storage.r2.access-key-id=test-access-key",
        "app.storage.r2.secret-access-key=test-secret-key",
        "app.storage.r2.bucket=chat-test",
        "app.storage.r2.public-base-url=https://cdn.example.com",
        "app.storage.r2.presigned-upload-ttl=10m",
        "app.storage.r2.max-file-size-bytes=10485760",
        "app.storage.r2.allowed-content-types=image/png,application/pdf"
})
class CloudflareR2ObjectStorageServiceTest {

    @Autowired
    private ObjectStorageService objectStorageService;

    @Test
    void createPresignedUploadReturnsUploadAndPublicUrls() {
        PresignedUploadResponse response = objectStorageService.createPresignedUpload(
                new CreatePresignedUploadRequest(
                        "report final.png",
                        "image/png",
                        1024L,
                        UploadPurpose.MESSAGE_ATTACHMENT
                )
        );

        assertThat(response.purpose()).isEqualTo(UploadPurpose.MESSAGE_ATTACHMENT);
        assertThat(response.objectKey()).startsWith("message-attachments/");
        assertThat(response.objectKey()).endsWith("report-final.png");
        assertThat(response.uploadUrl()).contains(".r2.cloudflarestorage.com");
        assertThat(response.uploadUrl()).contains("X-Amz-Algorithm");
        assertThat(response.publicUrl()).startsWith("https://cdn.example.com/message-attachments/");
        assertThat(response.method()).isEqualTo("PUT");
        assertThat(response.requiredHeaders()).containsEntry("Content-Type", "image/png");
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    void createPresignedUploadRejectsUnsupportedContentType() {
        assertThatThrownBy(() -> objectStorageService.createPresignedUpload(
                new CreatePresignedUploadRequest(
                        "clip.mp4",
                        "video/mp4",
                        1024L,
                        UploadPurpose.MESSAGE_ATTACHMENT
                )
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("contentType is not allowed: video/mp4");
    }

    @Test
    void createPresignedUploadRejectsOversizedFile() {
        assertThatThrownBy(() -> objectStorageService.createPresignedUpload(
                new CreatePresignedUploadRequest(
                        "large.pdf",
                        "application/pdf",
                        10485761L,
                        UploadPurpose.MESSAGE_ATTACHMENT
                )
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("sizeBytes exceeds max allowed size 10485760");
    }
}
