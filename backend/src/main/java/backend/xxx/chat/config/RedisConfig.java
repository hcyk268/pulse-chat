package backend.xxx.chat.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;

@Configuration
@EnableCaching
public class RedisConfig {

    public static final String USER_DETAILS_CACHE = "userDetailsByUsername";
    public static final String CACHED_USER_BY_USERNAME_CACHE = "cachedUserByUsername";
    public static final String CACHED_CONVERSATION_PARTICIPANTS_CACHE = "conversationParticipantsByConversationId";
    public static final String MARKET_OVERVIEW_CACHE = "marketOverview";
    public static final String MARKET_COIN_DETAIL_CACHE = "marketCoinDetailBySymbol";
    public static final String COMMUNITY_CATEGORIES_CACHE = "communityCategories";
    public static final String COMMUNITY_TAGS_CACHE = "communityTags";
    public static final String COMMUNITY_DISCOVERY_CACHE = "communityDiscovery";
    public static final String COMMUNITY_DETAIL_CACHE = "communityDetailBySlug";
    public static final String CONVERSATION_PINNED_MESSAGES_CACHE = "conversationPinnedMessagesByConversationId";

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = jsonSerializer(objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            @Value("${app.auth.user-details-cache-ttl}") Duration userDetailsCacheTtl,
            @Value("${app.auth.user-lookup-cache-ttl}") Duration userLookupCacheTtl,
            @Value("${app.conversation.participants-cache-ttl}") Duration conversationParticipantsCacheTtl,
            @Value("${app.conversation.pinned-messages-cache-ttl}") Duration conversationPinnedMessagesCacheTtl,
            @Value("${app.market.overview-cache-ttl}") Duration marketOverviewCacheTtl,
            @Value("${app.market.coin-detail-cache-ttl}") Duration marketCoinDetailCacheTtl,
            @Value("${app.community.categories-cache-ttl}") Duration communityCategoriesCacheTtl,
            @Value("${app.community.tags-cache-ttl}") Duration communityTagsCacheTtl,
            @Value("${app.community.discovery-cache-ttl}") Duration communityDiscoveryCacheTtl,
            @Value("${app.community.detail-cache-ttl}") Duration communityDetailCacheTtl
    ) {
        RedisCacheConfiguration baseConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer(objectMapper)));
        RedisCacheConfiguration userDetailsCacheConfiguration = baseConfiguration.entryTtl(userDetailsCacheTtl);
        RedisCacheConfiguration userLookupCacheConfiguration = baseConfiguration.entryTtl(userLookupCacheTtl);
        RedisCacheConfiguration conversationParticipantsCacheConfiguration =
                baseConfiguration.entryTtl(conversationParticipantsCacheTtl);
        RedisCacheConfiguration conversationPinnedMessagesCacheConfiguration =
                baseConfiguration.entryTtl(conversationPinnedMessagesCacheTtl);
        RedisCacheConfiguration marketOverviewCacheConfiguration = baseConfiguration.entryTtl(marketOverviewCacheTtl);
        RedisCacheConfiguration marketCoinDetailCacheConfiguration =
                baseConfiguration.entryTtl(marketCoinDetailCacheTtl);
        RedisCacheConfiguration communityCategoriesCacheConfiguration =
                baseConfiguration.entryTtl(communityCategoriesCacheTtl);
        RedisCacheConfiguration communityTagsCacheConfiguration = baseConfiguration.entryTtl(communityTagsCacheTtl);
        RedisCacheConfiguration communityDiscoveryCacheConfiguration =
                baseConfiguration.entryTtl(communityDiscoveryCacheTtl);
        RedisCacheConfiguration communityDetailCacheConfiguration = baseConfiguration.entryTtl(communityDetailCacheTtl);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(userDetailsCacheConfiguration)
                .withInitialCacheConfigurations(Map.ofEntries(
                        Map.entry(USER_DETAILS_CACHE, userDetailsCacheConfiguration),
                        Map.entry(CACHED_USER_BY_USERNAME_CACHE, userLookupCacheConfiguration),
                        Map.entry(CACHED_CONVERSATION_PARTICIPANTS_CACHE, conversationParticipantsCacheConfiguration),
                        Map.entry(CONVERSATION_PINNED_MESSAGES_CACHE, conversationPinnedMessagesCacheConfiguration),
                        Map.entry(MARKET_OVERVIEW_CACHE, marketOverviewCacheConfiguration),
                        Map.entry(MARKET_COIN_DETAIL_CACHE, marketCoinDetailCacheConfiguration),
                        Map.entry(COMMUNITY_CATEGORIES_CACHE, communityCategoriesCacheConfiguration),
                        Map.entry(COMMUNITY_TAGS_CACHE, communityTagsCacheConfiguration),
                        Map.entry(COMMUNITY_DISCOVERY_CACHE, communityDiscoveryCacheConfiguration),
                        Map.entry(COMMUNITY_DETAIL_CACHE, communityDetailCacheConfiguration)
                ))
                .build();
    }

    private GenericJackson2JsonRedisSerializer jsonSerializer(ObjectMapper objectMapper) {
        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }
}