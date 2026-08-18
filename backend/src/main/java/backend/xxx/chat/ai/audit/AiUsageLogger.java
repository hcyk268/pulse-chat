package backend.xxx.chat.ai.audit;

import backend.xxx.chat.ai.client.AiResponse;
import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiUsageLogger {

    public void logCompletion(
            AiExecutionContext context,
            AiResponse response,
            long latencyMs,
            int toolCalls
    ) {
        log.info(
                "ai.completed requestId={} useCase={} userId={} model={} latencyMs={} toolCalls={} promptTokens={} completionTokens={} totalTokens={}",
                context.requestId(),
                context.useCase(),
                context.currentUserId(),
                response.model(),
                latencyMs,
                toolCalls,
                response.usage() == null ? null : response.usage().promptTokens(),
                response.usage() == null ? null : response.usage().completionTokens(),
                response.usage() == null ? null : response.usage().totalTokens()
        );
    }
}