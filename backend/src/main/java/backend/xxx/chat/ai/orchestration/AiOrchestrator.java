package backend.xxx.chat.ai.orchestration;

import backend.xxx.chat.ai.audit.AiUsageLogger;
import backend.xxx.chat.ai.client.AiChatMessage;
import backend.xxx.chat.ai.client.AiClient;
import backend.xxx.chat.ai.client.AiRequest;
import backend.xxx.chat.ai.client.AiResponse;
import backend.xxx.chat.ai.client.AiStructuredRequest;
import backend.xxx.chat.ai.client.AiStructuredResponse;
import backend.xxx.chat.ai.prompt.AiPromptBuilder;
import backend.xxx.chat.ai.safety.SensitiveDataRedactor;
import backend.xxx.chat.config.properties.AIProperties;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiOrchestrator {

    private final AiClient aiClient;
    private final AIProperties properties;
    private final SensitiveDataRedactor redactor;
    private final AiPromptBuilder promptBuilder;
    private final AiUsageLogger usageLogger;

    public <T> AiStructuredResponse<T> completeStructuredTask(
            AiExecutionContext context,
            String instructions,
            Object input,
            Class<T> outputType
    ) {
        String inputJson = redactor.redactObject(input);
        List<AiChatMessage> messages = promptBuilder.buildStructuredTaskPrompt(
                context.useCase().name(),
                context.locale(),
                instructions,
                inputJson
        );
        AiStructuredRequest<T> request = new AiStructuredRequest<>(
                messages,
                properties.getMaxOutputTokens(),
                properties.getTemperature(),
                outputType,
                properties.isNativeStructuredOutput()
        );
        return completeStructuredAndLog(context, request, 0);
    }

    public AiResponse complete(
            AiExecutionContext context,
            AiRequest request,
            int toolCalls,
            List<ToolCallback> toolCallbacks,
            Map<String, Object> toolContext
    ) {
        return completeAndLog(context, request, toolCalls, toolCallbacks, toolContext);
    }

    private AiResponse completeAndLog(
            AiExecutionContext context,
            AiRequest request,
            int toolCalls,
            List<ToolCallback> toolCallbacks,
            Map<String, Object> toolContext
    ) {
        long startedAt = System.nanoTime();
        context.budget().acquireProviderCall();
        AiResponse response = aiClient.complete(request, toolCallbacks, toolContext);
        usageLogger.logCompletion(context, response, elapsedMillis(startedAt), toolCalls);
        return response;
    }

    private <T> AiStructuredResponse<T> completeStructuredAndLog(
            AiExecutionContext context,
            AiStructuredRequest<T> request,
            int toolCalls
    ) {
        long startedAt = System.nanoTime();
        context.budget().acquireProviderCall();
        AiStructuredResponse<T> response = aiClient.completeStructured(request);
        usageLogger.logCompletion(context, new AiResponse("", response.model(), response.usage()), elapsedMillis(startedAt), toolCalls);
        return response;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}