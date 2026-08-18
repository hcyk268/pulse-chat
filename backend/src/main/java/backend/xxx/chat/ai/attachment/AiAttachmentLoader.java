package backend.xxx.chat.ai.attachment;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.config.properties.AIProperties;
import backend.xxx.chat.config.properties.CloudflareR2Properties;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.message.model.MessageAttachment;
import backend.xxx.chat.message.repository.MessageAttachmentRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Component
@RequiredArgsConstructor
public class AiAttachmentLoader {

    private static final List<String> SUPPORTED_DOCUMENT_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "text/markdown",
            "application/json"
    );

    private final MessageAttachmentRepository messageAttachmentRepository;
    private final ConversationAccessPolicy conversationAccessPolicy;
    private final S3Client s3Client;
    private final CloudflareR2Properties r2Properties;
    private final AIProperties aiProperties;

    public List<LoadedAiAttachment> loadRequested(AiExecutionContext context, Collection<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalized = assetIds.stream().distinct().limit(aiProperties.getAttachment().getMaxFiles()).toList();
        List<MessageAttachment> attachments = messageAttachmentRepository.findByUploadedAssetIdInWithMessageAndConversation(normalized);
        return load(context, attachments);
    }

    private List<LoadedAiAttachment> load(AiExecutionContext context, List<MessageAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        List<LoadedAiAttachment> loaded = new ArrayList<>();
        for (MessageAttachment attachment : attachments) {
            Long conversationId = attachment.getMessage().getConversation().getId();
            conversationAccessPolicy.assertCanReadConversation(conversationId, context.currentUserId());
            String contentType = normalizeContentType(attachment.getContentType());
            if (!supported(contentType)) {
                continue;
            }
            validateSize(contentType, attachment.getSizeBytes());
            byte[] bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(r2Properties.bucket())
                            .key(attachment.getObjectKey())
                            .build())
                    .asByteArray();
            loaded.add(new LoadedAiAttachment(
                    attachment.getUploadedAsset().getId(),
                    attachment.getMessage().getId(),
                    conversationId,
                    attachment.getUploadedAsset().getOwner().getId(),
                    attachment.getObjectKey(),
                    attachment.getFileName(),
                    contentType,
                    attachment.getSizeBytes() == null ? bytes.length : attachment.getSizeBytes(),
                    bytes
            ));
        }
        return List.copyOf(loaded);
    }

    private boolean supported(String contentType) {
        return contentType.startsWith("image/") || SUPPORTED_DOCUMENT_TYPES.contains(contentType);
    }

    private void validateSize(String contentType, Long sizeBytes) {
        long size = sizeBytes == null ? 0L : sizeBytes;
        long max = contentType.startsWith("image/")
                ? aiProperties.getAttachment().getMaxImageBytes()
                : aiProperties.getAttachment().getMaxDocumentBytes();
        if (size <= 0 || size > max) {
            throw new ValidationException("ai.attachment.size.invalid");
        }
    }

    private String normalizeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType.trim().toLowerCase(Locale.ROOT) : "application/octet-stream";
    }
}