package backend.xxx.chat.ai.web;

import backend.xxx.chat.common.ratelimit.ClientIpResolver;
import backend.xxx.chat.common.ratelimit.RateLimitProvider;
import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.config.properties.AIProperties;
import backend.xxx.chat.user.service.UserLookupService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiRequestLimiter {

    private final AIProperties properties;
    private final RateLimitProvider rateLimitProvider;
    private final ClientIpResolver clientIpResolver;
    private final CurrentUserProvider currentUserProvider;
    private final UserLookupService userLookupService;

    public void check(HttpServletRequest request, String action) {
        String subject = aiRateLimitSubject(request);
        rateLimitProvider.rateLimit(
                subject,
                "ai:global",
                properties.getRateLimit().getGlobalMaxRequests(),
                properties.getRateLimit().getWindow()
        );
        rateLimitProvider.rateLimit(
                subject,
                "ai:" + action,
                properties.getRateLimit().getMaxRequests(),
                properties.getRateLimit().getWindow()
        );
    }

    private String aiRateLimitSubject(HttpServletRequest request) {
        try {
            String username = currentUserProvider.getCurrentUsername();
            if (StringUtils.hasText(username)) {
                Long userId = userLookupService.getCurrentUserId(username);
                if (userId != null) {
                    return "user:" + userId;
                }
                return "username:" + username.trim();
            }
        } catch (RuntimeException ignored) {
            // Fall back to IP-based limiting when authentication context is unavailable.
        }
        return "ip:" + clientIpResolver.resolve(request);
    }
}