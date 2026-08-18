package backend.xxx.chat.ai.client;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.ToolCallback;

public interface AiClient {

    AiResponse complete(AiRequest request);

    default AiResponse complete(AiRequest request, List<ToolCallback> toolCallbacks, Map<String, Object> toolContext) {
        return complete(request);
    }

    <T> AiStructuredResponse<T> completeStructured(AiStructuredRequest<T> request);
}