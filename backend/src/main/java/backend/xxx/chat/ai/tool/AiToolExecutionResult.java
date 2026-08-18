package backend.xxx.chat.ai.tool;

public record AiToolExecutionResult(
        String toolName,
        String argumentsJson,
        String resultJson
) {
}