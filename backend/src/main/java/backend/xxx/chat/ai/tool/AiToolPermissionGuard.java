package backend.xxx.chat.ai.tool;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiToolPermissionGuard {

    private final AiToolPolicy toolPolicy;

    public void assertCanExecute(AiTool<?, ?> tool, AiExecutionContext context) {
        if (tool.access() != AiToolAccess.READ_ONLY) {
            throw new ForbiddenException("ai.tool.write.confirmation.required");
        }
        if (context.currentUserId() == null) {
            throw new ForbiddenException("ai.tool.user.required");
        }
        if (!toolPolicy.isAllowed(context.useCase(), tool.name())) {
            throw new ForbiddenException("ai.tool.not.allowed.for.use-case");
        }
    }
}