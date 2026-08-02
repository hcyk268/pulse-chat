package backend.xxx.chat.auth.service;

import java.util.List;
import java.util.Locale;

import backend.xxx.chat.auth.model.AuthenticatedUser;
import backend.xxx.chat.config.RedisConfig;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(cacheNames = RedisConfig.USER_DETAILS_CACHE, key = "T(backend.xxx.chat.auth.service.CustomUserDetailsService).cacheKey(#username)")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("user.not.found: " + username));

        return toUserDetails(user);
    }

    public UserDetails toUserDetails(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getAccountStatus(),
                user.isEmailVerified(),
                user.getCredentialsVersion(),
                List.of(new SimpleGrantedAuthority(DEFAULT_ROLE))
        );
    }

    public void evictUserDetails(User user) {
        if (user == null) {
            return;
        }

        evictUserDetails(user.getUsername());
        evictUserDetails(user.getEmail());
    }

    public void evictUserDetails(String usernameOrEmail) {
        Cache cache = cacheManager.getCache(RedisConfig.USER_DETAILS_CACHE);
        if (cache == null || usernameOrEmail == null) {
            return;
        }

        cache.evict(cacheKey(usernameOrEmail));
    }

    public static String cacheKey(String usernameOrEmail) {
        return usernameOrEmail.trim().toLowerCase(Locale.ROOT);
    }
}
