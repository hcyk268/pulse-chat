package backend.xxx.chat.config;

import backend.xxx.chat.common.exception.ValidationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(CloudflareR2Properties.class)
public class CloudflareR2Config {

    @Bean
    @ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true")
    public S3Presigner s3Presigner(CloudflareR2Properties properties) {
        validateRequiredProperties(properties);

        return S3Presigner.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of("auto"))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true")
    public S3Client s3Client(CloudflareR2Properties properties) {
        validateRequiredProperties(properties);

        return S3Client.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of("auto"))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    private StaticCredentialsProvider credentialsProvider(CloudflareR2Properties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        properties.accessKeyId(),
                        properties.secretAccessKey()
                )
        );
    }

    private void validateRequiredProperties(CloudflareR2Properties properties) {
        requireNotBlank(properties.accountId(), "R2 accountId must not be blank");
        requireNotBlank(properties.accessKeyId(), "R2 accessKeyId must not be blank");
        requireNotBlank(properties.secretAccessKey(), "R2 secretAccessKey must not be blank");
        requireNotBlank(properties.bucket(), "R2 bucket must not be blank");
        requireNotBlank(properties.publicBaseUrl(), "R2 publicBaseUrl must not be blank");
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}