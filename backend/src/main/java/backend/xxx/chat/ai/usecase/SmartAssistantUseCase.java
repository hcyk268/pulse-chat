package backend.xxx.chat.ai.usecase;

import backend.xxx.chat.ai.attachment.AiAttachmentContext;
import backend.xxx.chat.ai.attachment.AiAttachmentContextBuilder;
import backend.xxx.chat.ai.attachment.AiAttachmentLoader;
import backend.xxx.chat.ai.attachment.LoadedAiAttachment;
import backend.xxx.chat.ai.client.AiChatMessage;
import backend.xxx.chat.ai.client.AiRequest;
import backend.xxx.chat.ai.client.AiResponse;
import backend.xxx.chat.ai.config.AiDefaults;
import backend.xxx.chat.ai.dto.AiToolCallResponse;
import backend.xxx.chat.ai.dto.SmartAssistantRequest;
import backend.xxx.chat.ai.dto.SmartAssistantResponse;
import backend.xxx.chat.ai.memory.AiChatMemoryService;
import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.orchestration.AiExecutionContextFactory;
import backend.xxx.chat.ai.orchestration.AiOrchestrator;
import backend.xxx.chat.ai.orchestration.AiUseCaseType;
import backend.xxx.chat.ai.prompt.PromptBudgeter;
import backend.xxx.chat.ai.prompt.SmartAssistantPromptFactory;
import backend.xxx.chat.ai.safety.AiResponseValidator;
import backend.xxx.chat.ai.safety.SensitiveDataRedactor;
import backend.xxx.chat.ai.tool.AiToolCallbackFactory;
import backend.xxx.chat.ai.tool.AiToolPolicy;
import backend.xxx.chat.config.properties.AIProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SmartAssistantUseCase {

    private final AiExecutionContextFactory contextFactory;
    private final AiOrchestrator orchestrator;
    private final AiResponseValidator responseValidator;
    private final SensitiveDataRedactor redactor;
    private final AIProperties properties;
    private final PromptBudgeter promptBudgeter;
    private final SmartAssistantPromptFactory promptFactory;
    private final AiToolPolicy toolPolicy;
    private final AiToolCallbackFactory toolCallbackFactory;
    private final AiAttachmentLoader attachmentLoader;
    private final AiAttachmentContextBuilder attachmentContextBuilder;
    private final AiChatMemoryService chatMemoryService;

    public SmartAssistantResponse answer(SmartAssistantRequest request) {
        AiExecutionContext context = contextFactory.create(AiUseCaseType.SMART_ASSISTANT);
        List<AiToolCallResponse> toolCalls = Collections.synchronizedList(new ArrayList<>());
        List<ToolCallback> toolCallbacks = toolCallbackFactory.createCallbacks(
                context,
                allowedToolNames(request),
                toolCalls
        );

        AiAttachmentContext attachmentContext = attachmentContext(request, context);
        String userPrompt = userPrompt(request, attachmentContext);
        List<AiChatMessage> messages = new ArrayList<>();
        messages.add(AiChatMessage.system(promptFactory.systemPrompt(context.locale())));
        messages.addAll(chatMemoryService.load(context, request.conversationId()));
        messages.add(AiChatMessage.user(userPrompt, attachmentContext.imageMedia()));

        AiResponse response = orchestrator.complete(
                context,
                new AiRequest(
                        promptBudgeter.fitMessages(messages, AiDefaults.MAX_INPUT_CHARS),
                        properties.getMaxOutputTokens(),
                        properties.getTemperature(),
                        false
                ),
                toolCalls.size(),
                toolCallbacks,
                toolContext(context)
        );
        String answer = redactor.redact(responseValidator.requiredPlainText(response.content(), 4_000));
        chatMemoryService.append(context, request.conversationId(), redactor.redact(request.question()), answer);

        return new SmartAssistantResponse(
                answer,
                List.copyOf(toolCalls),
                Instant.now(),
                response.model()
        );
    }

    private AiAttachmentContext attachmentContext(SmartAssistantRequest request, AiExecutionContext context) {
        List<Long> attachmentIds = request.normalizedAttachmentIds();
        if (attachmentIds.isEmpty()) {
            return AiAttachmentContext.empty();
        }
        List<LoadedAiAttachment> attachments = attachmentLoader.loadRequested(context, attachmentIds);
        return attachmentContextBuilder.build(attachments);
    }

    private String userPrompt(SmartAssistantRequest request, AiAttachmentContext attachmentContext) {
        String prompt = promptFactory.initialUserPrompt(request);
        if (!StringUtils.hasText(attachmentContext.textContext())) {
            return prompt;
        }
        return prompt + "\n\nAttachment extracted text:\n" + attachmentContext.textContext();
    }

    private Set<String> allowedToolNames(SmartAssistantRequest request) {
        Set<String> allowed = new HashSet<>();
        Set<String> policyAllowed = toolPolicy.allowedToolNames(AiUseCaseType.SMART_ASSISTANT);
        addIfPolicyAllows(allowed, policyAllowed, "getCurrentUser");
        addIfPolicyAllows(allowed, policyAllowed, "getMarket");
        if (request.conversationId() != null) {
            addIfPolicyAllows(allowed, policyAllowed, "getConversationMessages");
            addIfPolicyAllows(allowed, policyAllowed, "getPinnedMessages");
            addIfPolicyAllows(allowed, policyAllowed, "searchMessagesByKeyword");
        }
        if (StringUtils.hasText(request.symbol())) {
            addIfPolicyAllows(allowed, policyAllowed, "getCoinDetail");
            addIfPolicyAllows(allowed, policyAllowed, "getTickerCandles");
        }
        if (StringUtils.hasText(request.communitySlug())) {
            addIfPolicyAllows(allowed, policyAllowed, "getCommunityDetail");
        }
        return allowed;
    }

    private void addIfPolicyAllows(Set<String> allowed, Set<String> policyAllowed, String toolName) {
        if (policyAllowed.contains(toolName)) {
            allowed.add(toolName);
        }
    }

    private java.util.Map<String, Object> toolContext(AiExecutionContext context) {
        return java.util.Map.of(
                "requestId", context.requestId(),
                "useCase", context.useCase().name(),
                "currentUserId", context.currentUserId(),
                "locale", context.locale()
        );
    }
}
