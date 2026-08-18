package backend.xxx.chat.ai.tool.impl;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.tool.AiTool;
import backend.xxx.chat.ai.tool.AiToolAccess;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SearchMessagesByKeywordTool implements AiTool<SearchMessagesByKeywordTool.Input, SearchMessagesByKeywordTool.Output> {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final ConversationAccessPolicy conversationAccessPolicy;
    private final MessageRepository messageRepository;

    @Override
    public String name() {
        return "searchMessagesByKeyword";
    }

    @Override
    public String description() {
        return "Search messages by keyword inside one conversation the current user can read.";
    }

    @Override
    public String argumentSchema() {
        return "{\"conversationId\": number, \"keyword\": string, \"limit\": number optional 1..20}";
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public AiToolAccess access() {
        return AiToolAccess.READ_ONLY;
    }

    @Override
    @Transactional(readOnly = true)
    public Output execute(Input input, AiExecutionContext context) {
        if (input.conversationId() == null) {
            throw new ValidationException("conversationId must not be null");
        }
        if (input.keyword() == null || input.keyword().isBlank()) {
            throw new ValidationException("keyword must not be blank");
        }

        conversationAccessPolicy.assertCanReadConversation(input.conversationId(), context.currentUserId());
        int limit = normalizeLimit(input.limit());
        String keyword = normalizeKeyword(input.keyword());
        List<MessageItem> messages = messageRepository.searchByConversationIdAndKeyword(
                        input.conversationId(),
                        escapeLikeKeyword(keyword),
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(this::toItem)
                .toList();
        return new Output(input.conversationId(), keyword, messages);
    }

    private MessageItem toItem(Message message) {
        return new MessageItem(
                message.getId(),
                message.getSender() == null ? null : message.getSender().getId(),
                message.getSender() == null ? null : message.getSender().getDisplayName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword.trim();
    }

    private String escapeLikeKeyword(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    public record Input(Long conversationId, String keyword, Integer limit) {
    }

    public record Output(Long conversationId, String keyword, List<MessageItem> messages) {
    }

    public record MessageItem(
            Long id,
            Long senderId,
            String senderDisplayName,
            String content,
            Instant createdAt
    ) {
    }
}