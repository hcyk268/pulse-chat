package backend.xxx.chat.ai.tool.impl;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.tool.AiTool;
import backend.xxx.chat.ai.tool.AiToolAccess;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetPinnedMessagesTool implements AiTool<GetPinnedMessagesTool.Input, GetPinnedMessagesTool.Output> {

    private final MessageService messageService;

    @Override
    public String name() {
        return "getPinnedMessages";
    }

    @Override
    public String description() {
        return "Return pinned messages from a conversation the current user can read.";
    }

    @Override
    public String argumentSchema() {
        return "{\"conversationId\": number}";
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
        var response = messageService.getPinnedMessages(context.currentUsername(), input.conversationId());
        List<PinnedMessageItem> items = response.items()
                .stream()
                .filter(pin -> pin.message() != null && pin.message().deletedAt() == null)
                .map(this::toItem)
                .toList();
        return new Output(response.conversationId(), items);
    }

    private PinnedMessageItem toItem(MessagePinResponse pin) {
        MessageResponse message = pin.message();
        return new PinnedMessageItem(
                pin.pinId(),
                message.id(),
                message.sender() == null ? null : message.sender().id(),
                message.sender() == null ? null : message.sender().displayName(),
                message.content(),
                message.messageType() == null ? null : message.messageType().name(),
                pin.pinnedAt(),
                message.editedAt()
        );
    }

    public record Input(Long conversationId) {
    }

    public record Output(Long conversationId, List<PinnedMessageItem> items) {
    }

    public record PinnedMessageItem(
            Long pinId,
            Long messageId,
            Long senderId,
            String senderDisplayName,
            String content,
            String messageType,
            Instant pinnedAt,
            Instant editedAt
    ) {
    }
}