package backend.xxx.chat.user.service;

import java.util.Locale;

import backend.xxx.chat.common.exception.UnauthorizedException;
import backend.xxx.chat.config.RedisConfig;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLookupCacheService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    @Cacheable(cacheNames = RedisConfig.CACHED_USER_BY_USERNAME_CACHE, key = "T(backend.xxx.chat.user.service.UserLookupCacheService).cacheKey(#username)")
    public CachedUser getCurrentUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(CachedUser::from)
                .orElseThrow(() -> new UnauthorizedException("user.current.not.found"));
    }

    public void evictUser(User user) {
        if (user == null) {
            return;
        }

        evictUser(user.getUsername());
        evictUser(user.getEmail());
    }

    public void evictUser(String usernameOrEmail) {
        Cache cache = cacheManager.getCache(RedisConfig.CACHED_USER_BY_USERNAME_CACHE);
        if (cache == null || usernameOrEmail == null) {
            return;
        }

        cache.evict(cacheKey(usernameOrEmail));
    }

    public static String cacheKey(String usernameOrEmail) {
        return usernameOrEmail.trim().toLowerCase(Locale.ROOT);
    }
}
