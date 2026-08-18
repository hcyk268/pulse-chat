package backend.xxx.chat.ai.tool;

import backend.xxx.chat.ai.dto.AiToolCallResponse;
import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.usecase.SmartAssistantToolRunner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiToolCallbackFactory {

    private final AiToolRegistry toolRegistry;
    private final SmartAssistantToolRunner toolRunner;
    private final ObjectMapper objectMapper;

    public List<ToolCallback> createCallbacks(
            AiExecutionContext context,
            Set<String> allowedToolNames,
            List<AiToolCallResponse> toolCalls
    ) {
        return toolRegistry.readOnlyTools(allowedToolNames)
                .stream()
                .map(tool -> callback(tool, context, toolCalls))
                .toList();
    }

    private ToolCallback callback(
            AiTool<?, ?> tool,
            AiExecutionContext context,
            List<AiToolCallResponse> toolCalls
    ) {
        ToolDefinition definition = DefaultToolDefinition.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(inputSchema(tool))
                .build();
        ToolMetadata metadata = ToolMetadata.builder().returnDirect(false).build();
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return metadata;
            }

            @Override
            public String call(String toolInput) {
                return call(toolInput, null);
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                JsonNode arguments = parseArguments(toolInput);
                AiToolExecutionResult result = toolRunner.executeToolForAssistant(tool.name(), arguments, context);
                toolCalls.add(new AiToolCallResponse(result.toolName(), result.argumentsJson()));
                return result.resultJson();
            }
        };
    }

    private String inputSchema(AiTool<?, ?> tool) {
        String generated = JsonSchemaGenerator.generateForType(tool.inputType());
        if (StringUtils.hasText(generated)) {
            return generated;
        }
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    private JsonNode parseArguments(String toolInput) {
        if (!StringUtils.hasText(toolInput)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(toolInput);
        } catch (JsonProcessingException ex) {
            return objectMapper.createObjectNode();
        }
    }
}