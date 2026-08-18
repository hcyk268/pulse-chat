package backend.xxx.chat.ai.orchestration;

import backend.xxx.chat.ai.config.AiDefaults;
import java.util.UUID;

import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.user.service.CachedUser;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiExecutionContextFactory {

    private final CurrentUserProvider currentUserProvider;
    private final UserLookupService userLookupService;

    public AiExecutionContext create(AiUseCaseType useCase) {
        String username = currentUserProvider.getCurrentUsername();
        CachedUser currentUser = userLookupService.getCurrentUserCached(username);
        return new AiExecutionContext(
                UUID.randomUUID().toString(),
                useCase,
                new AiExecutionBudget(
                        AiDefaults.MAX_PROVIDER_CALLS_PER_REQUEST,
                        AiDefaults.WORKFLOW_TIMEOUT
                ),
                currentUser.id(),
                currentUser.username(),
                currentUser.displayName(),
                LocaleContextHolder.getLocale().toLanguageTag()
        );
    }
}