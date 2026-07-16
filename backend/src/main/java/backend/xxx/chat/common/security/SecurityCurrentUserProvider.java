package backend.xxx.chat.common.security;

import backend.xxx.chat.common.exception.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("auth.unauthorized");
        }

        return authentication.getName();
    }
}
