package backend.xxx.chat.ai.tool.impl;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.tool.AiTool;
import backend.xxx.chat.ai.tool.AiToolAccess;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.message.dto.MessageHistoryResponse;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetConversationMessagesTool implements AiTool<GetConversationMessagesTool.Input, GetConversationMessagesTool.Output> {

    private static final short DEFAULT_LIMIT = 30;
    private static final short MAX_LIMIT = 50;

    private final MessageService messageService;

    @Override
    public String name() {
        return "getConversationMessages";
    }

    @Override
    public String description() {
        return "Return recent messages from a conversation the current user can read.";
    }

    @Override
    public String argumentSchema() {
        return "{\"conversationId\": number, \"limit\": number optional 1..50}";
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
    public Output execute(Input input, AiExecutionContext context) {
        if (input.conversationId() == null) {
            throw new ValidationException("conversationId must not be null");
        }
        short limit = normalizeLimit(input.limit());
        MessageHistoryResponse history = messageService.getHistory(
                context.currentUsername(),
                input.conversationId(),
                limit,
                null
        );
        List<MessageItem> items = history.items()
                .stream()
                .filter(message -> message.deletedAt() == null)
                .map(this::toItem)
                .toList();
        return new Output(input.conversationId(), items);
    }

    private MessageItem toItem(MessageResponse message) {
        return new MessageItem(
                message.id(),
                message.sender() == null ? null : message.sender().id(),
                message.sender() == null ? null : message.sender().displayName(),
                message.content(),
                message.messageType() == null ? null : message.messageType().name(),
                message.createdAt(),
                message.editedAt(),
                message.attachments() == null ? 0 : message.attachments().size()
        );
    }

    private short normalizeLimit(Short limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return (short) Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    public record Input(Long conversationId, Short limit) {
    }

    public record Output(Long conversationId, List<MessageItem> messages) {
    }

    public record MessageItem(
            Long id,
            Long senderId,
            String senderDisplayName,
            String content,
            String messageType,
            Instant createdAt,
            Instant editedAt,
            int attachmentCount
    ) {
    }
}