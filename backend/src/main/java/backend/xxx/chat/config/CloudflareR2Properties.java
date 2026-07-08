package backend.xxx.chat.config;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.r2")
public record CloudflareR2Properties(
        boolean enabled,
        String accountId,
        String accessKeyId,
        String secretAccessKey,
        String bucket,
        String publicBaseUrl,
        Duration presignedUploadTtl,
        long maxFileSizeBytes,
        List<String> allowedContentTypes
) {

    public URI endpoint() {
        return URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
    }
}
