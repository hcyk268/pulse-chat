package backend.xxx.chat.ai.attachment;

import backend.xxx.chat.ai.client.AiMedia;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiAttachmentContextBuilder {

    private final AiDocumentExtractor documentExtractor;

    public AiAttachmentContext build(List<LoadedAiAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return AiAttachmentContext.empty();
        }
        List<AiMedia> images = new ArrayList<>();
        List<AiAttachmentSource> sources = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (LoadedAiAttachment attachment : attachments) {
            sources.add(toSource(attachment));
            if (attachment.image()) {
                images.add(new AiMedia(attachment.contentType(), attachment.fileName(), attachment.data()));
                continue;
            }
            String extracted = documentExtractor.extract(attachment);
            if (StringUtils.hasText(extracted)) {
                text.append("\n<attachment assetId=\"")
                        .append(attachment.assetId())
                        .append("\" fileName=\"")
                        .append(safe(attachment.fileName()))
                        .append("\" contentType=\"")
                        .append(safe(attachment.contentType()))
                        .append("\">\n")
                        .append(extracted)
                        .append("\n</attachment>\n");
            }
        }
        return new AiAttachmentContext(text.toString().trim(), List.copyOf(images), List.copyOf(sources));
    }

    private AiAttachmentSource toSource(LoadedAiAttachment attachment) {
        return new AiAttachmentSource(
                attachment.assetId(),
                attachment.messageId(),
                attachment.conversationId(),
                attachment.fileName(),
                attachment.contentType(),
                attachment.image() ? "image" : "document"
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.replace((char) 34, (char) 39);
    }
}
