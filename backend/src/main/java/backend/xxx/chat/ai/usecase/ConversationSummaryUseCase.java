package backend.xxx.chat.ai.usecase;

import backend.xxx.chat.ai.config.AiDefaults;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import backend.xxx.chat.ai.dto.ConversationSummaryRequest;
import backend.xxx.chat.ai.dto.ConversationSummaryResponse;
import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.orchestration.AiExecutionContextFactory;
import backend.xxx.chat.ai.orchestration.AiOrchestrator;
import backend.xxx.chat.ai.orchestration.AiUseCaseType;
import backend.xxx.chat.ai.prompt.InstructionPrompt;
import backend.xxx.chat.ai.prompt.PromptBudgeter;
import backend.xxx.chat.ai.safety.AiResponseValidator;
import backend.xxx.chat.ai.safety.SensitiveDataRedactor;
import backend.xxx.chat.conversation.dto.ConversationPinnedMessagesResponse;
import backend.xxx.chat.message.dto.MessageHistoryResponse;
import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationSummaryUseCase {

    private static final int MESSAGE_CONTENT_MAX_CHARS = 900;
    private static final int PINNED_MESSAGE_CONTENT_MAX_CHARS = 700;

    private final AiExecutionContextFactory contextFactory;
    private final AiOrchestrator orchestrator;
    private final AiResponseValidator responseValidator;
    private final SensitiveDataRedactor redactor;
    private final PromptBudgeter promptBudgeter;
    private final MessageService messageService;

    public ConversationSummaryResponse summarize(Long conversationId, ConversationSummaryRequest request) {
        AiExecutionContext context = contextFactory.create(AiUseCaseType.CONVERSATION_SUMMARY);
        short limit = request == null ? 50 : request.normalizedLimit();

        MessageHistoryResponse history = messageService.getHistory(context.currentUsername(), conversationId, limit, null);
        ConversationPinnedMessagesResponse pinnedMessages = messageService.getPinnedMessages(
                context.currentUsername(),
                conversationId
        );
        List<MessageInput> messages = budgetMessages(history.items()
                .stream()
                .filter(message -> message.deletedAt() == null)
                .map(this::toMessageInput)
                .toList());
        List<PinnedMessageInput> pinnedInputs = pinnedMessages.items()
                .stream()
                .filter(pin -> pin.message() != null && pin.message().deletedAt() == null)
                .map(this::toPinnedMessageInput)
                .toList();
        return generateSummary(context, conversationId, messages, pinnedInputs);
    }

    private ConversationSummaryResponse generateSummary(
            AiExecutionContext context,
            Long conversationId,
            List<MessageInput> messages,
            List<PinnedMessageInput> pinnedMessages
    ) {
        SummaryInput input = new SummaryInput(conversationId, messages, pinnedMessages);
        var structured = orchestrator.completeStructuredTask(
                context,
                InstructionPrompt.CONVERSATION_SUMMARY_PROMPT,
                input,
                ConversationSummaryOutput.class
        );
        ConversationSummaryOutput output = structured.entity();
        return new ConversationSummaryResponse(
                conversationId,
                redactor.redact(responseValidator.requiredPlainText(output.summary(), 2_000)),
                redactor.redactAll(boundedList(output.highlights(), 8, 240)),
                redactor.redactAll(boundedList(output.actionItems(), 8, 240)),
                Instant.now(),
                structured.model()
        );
    }

    private List<String> boundedList(List<String> values, int maxItems, int maxItemLength) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(maxItems)
                .map(value -> value.length() <= maxItemLength ? value.trim() : value.substring(0, maxItemLength).trim())
                .toList();
    }
    private List<MessageInput> budgetMessages(List<MessageInput> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }

        int charBudget = Math.max(2_000, AiDefaults.MAX_INPUT_CHARS * 2 / 3);
        int usedChars = 0;
        List<MessageInput> selected = new ArrayList<>();
        for (int index = messages.size() - 1; index >= 0; index--) {
            MessageInput candidate = trimMessage(messages.get(index), MESSAGE_CONTENT_MAX_CHARS);
            int estimate = estimateChars(candidate);
            if (!selected.isEmpty() && usedChars + estimate > charBudget) {
                break;
            }
            selected.add(0, candidate);
            usedChars += estimate;
        }
        return selected.isEmpty()
                ? List.of(trimMessage(messages.get(messages.size() - 1), MESSAGE_CONTENT_MAX_CHARS))
                : List.copyOf(selected);
    }

    private MessageInput trimMessage(MessageInput message, int maxContentChars) {
        return new MessageInput(
                message.id(),
                message.sender(),
                promptBudgeter.truncateTail(message.content(), maxContentChars),
                message.messageType(),
                message.createdAt(),
                message.editedAt()
        );
    }

    private int estimateChars(MessageInput message) {
        return 160 + (message.content() == null ? 0 : message.content().length());
    }

    private MessageInput toMessageInput(MessageResponse message) {
        return new MessageInput(
                message.id(),
                message.sender() == null ? null : message.sender().displayName(),
                message.content(),
                message.messageType() == null ? null : message.messageType().name(),
                message.createdAt(),
                message.editedAt()
        );
    }

    private PinnedMessageInput toPinnedMessageInput(MessagePinResponse pin) {
        MessageResponse message = pin.message();
        return new PinnedMessageInput(
                pin.pinId(),
                message.id(),
                message.sender() == null ? null : message.sender().displayName(),
                promptBudgeter.truncateTail(message.content(), PINNED_MESSAGE_CONTENT_MAX_CHARS),
                pin.pinnedAt(),
                message.editedAt()
        );
    }

    private record SummaryInput(
            Long conversationId,
            List<MessageInput> messages,
            List<PinnedMessageInput> pinnedMessages
    ) {
    }

    private record MessageInput(
            Long id,
            String sender,
            String content,
            String messageType,
            Instant createdAt,
            Instant editedAt
    ) {
    }

    private record PinnedMessageInput(
            Long pinId,
            Long messageId,
            String sender,
            String content,
            Instant pinnedAt,
            Instant editedAt
    ) {
    }
}