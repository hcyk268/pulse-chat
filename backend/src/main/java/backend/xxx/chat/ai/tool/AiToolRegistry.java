package backend.xxx.chat.ai.tool;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import backend.xxx.chat.common.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class AiToolRegistry {

    private final Map<String, AiTool<?, ?>> toolsByName;

    public AiToolRegistry(List<AiTool<?, ?>> tools) {
        this.toolsByName = tools.stream()
                .sorted(Comparator.comparing(AiTool::name))
                .collect(Collectors.toMap(
                        AiTool::name,
                        tool -> tool,
                        (current, duplicate) -> current,
                        LinkedHashMap::new
                ));
    }

    public AiTool<?, ?> requireTool(String name) {
        AiTool<?, ?> tool = toolsByName.get(name);
        if (tool == null) {
            throw new ValidationException("ai.tool.unsupported");
        }
        return tool;
    }

    public String describeReadOnlyTools() {
        return describeReadOnlyTools(toolsByName.keySet(), ignored -> "read-only backend data");
    }

    public List<AiTool<?, ?>> readOnlyTools(Set<String> allowedToolNames) {
        return toolsByName.values()
                .stream()
                .filter(tool -> tool.access() == AiToolAccess.READ_ONLY)
                .filter(tool -> allowedToolNames.contains(tool.name()))
                .toList();
    }
    public String describeReadOnlyTools(Set<String> allowedToolNames, Function<String, String> outputScopeProvider) {
        return toolsByName.values()
                .stream()
                .filter(tool -> tool.access() == AiToolAccess.READ_ONLY)
                .filter(tool -> allowedToolNames.contains(tool.name()))
                .map(tool -> "- " + tool.name() + ": " + tool.description()
                        + " Arguments JSON schema: " + tool.argumentSchema()
                        + " Output scope: " + outputScopeProvider.apply(tool.name()))
                .collect(Collectors.joining("\n"));
    }
}