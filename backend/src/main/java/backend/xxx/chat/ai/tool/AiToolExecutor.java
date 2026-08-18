package backend.xxx.chat.ai.tool;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.safety.SensitiveDataRedactor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiToolExecutor {

    private final AiToolRegistry toolRegistry;
    private final AiToolPermissionGuard permissionGuard;
    private final SensitiveDataRedactor redactor;
    private final ObjectMapper objectMapper;

    public AiToolExecutionResult execute(String toolName, JsonNode arguments, AiExecutionContext context) {
        AiTool<?, ?> rawTool = toolRegistry.requireTool(toolName);
        permissionGuard.assertCanExecute(rawTool, context);
        return executeTyped(rawTool, arguments, context);
    }

    private <I, O> AiToolExecutionResult executeTyped(
            AiTool<I, O> tool,
            JsonNode arguments,
            AiExecutionContext context
    ) {
        I input = objectMapper.convertValue(arguments == null || arguments.isMissingNode()
                ? objectMapper.createObjectNode()
                : arguments, tool.inputType());
        O output = tool.execute(input, context);
        return new AiToolExecutionResult(
                tool.name(),
                redactor.redact(arguments == null ? "{}" : arguments.toString()),
                redactor.redactObject(output)
        );
    }
}