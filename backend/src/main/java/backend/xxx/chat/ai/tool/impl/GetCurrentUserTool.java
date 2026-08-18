package backend.xxx.chat.ai.tool.impl;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.tool.AiTool;
import backend.xxx.chat.ai.tool.AiToolAccess;
import backend.xxx.chat.user.service.CachedUser;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCurrentUserTool implements AiTool<GetCurrentUserTool.Input, GetCurrentUserTool.Output> {

    private final UserLookupService userLookupService;

    @Override
    public String name() {
        return "getCurrentUser";
    }

    @Override
    public String description() {
        return "Return the authenticated user's non-sensitive profile.";
    }

    @Override
    public String argumentSchema() {
        return "{}";
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public AiToolAccess access() {
        return AiToolAccess.READ_ONLY;
    }

    @Override
    public Output execute(Input input, AiExecutionContext context) {
        CachedUser user = userLookupService.getCurrentUserCached(context.currentUsername());
        return new Output(
                user.id(),
                user.username(),
                user.displayName(),
                user.avatarUrl(),
                user.bio(),
                user.accountStatus() == null ? null : user.accountStatus().name(),
                user.emailVerified()
        );
    }

    public record Input() {
    }

    public record Output(
            Long id,
            String username,
            String displayName,
            String avatarUrl,
            String bio,
            String accountStatus,
            boolean emailVerified
    ) {
    }
}