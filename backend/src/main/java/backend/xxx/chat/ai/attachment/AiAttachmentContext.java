package backend.xxx.chat.ai.attachment;

import backend.xxx.chat.ai.client.AiMedia;
import java.util.List;

public record AiAttachmentContext(
        String textContext,
        List<AiMedia> imageMedia,
        List<AiAttachmentSource> sources
) {
    public static AiAttachmentContext empty() {
        return new AiAttachmentContext("", List.of(), List.of());
    }
}