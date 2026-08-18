package backend.xxx.chat.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SmartAssistantRequest(
        @NotBlank @Size(max = 2_000) String question,
        Long conversationId,
        @Size(max = 120) String communitySlug,
        @Size(max = 30) String symbol,
        @Size(max = 10) List<@Positive Long> attachmentIds
) {
    public SmartAssistantRequest(String question, Long conversationId, String communitySlug, String symbol) {
        this(question, conversationId, communitySlug, symbol, List.of());
    }

    public List<Long> normalizedAttachmentIds() {
        return attachmentIds == null ? List.of() : attachmentIds.stream().distinct().toList();
    }
}