package backend.xxx.chat.community.service;

import java.util.List;

import backend.xxx.chat.config.RedisConfig;
import backend.xxx.chat.community.dto.CommunityCategoryResponse;
import backend.xxx.chat.community.dto.CommunityTagResponse;
import backend.xxx.chat.community.model.CommunityCategory;
import backend.xxx.chat.community.model.CommunityTag;
import backend.xxx.chat.community.repository.CommunityCategoryRepository;
import backend.xxx.chat.community.repository.CommunityChannelRepository;
import backend.xxx.chat.community.repository.CommunityMemberRepository;
import backend.xxx.chat.community.repository.CommunityRepository;
import backend.xxx.chat.community.repository.CommunityTagLinkRepository;
import backend.xxx.chat.community.repository.CommunityTagRepository;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.notification.service.NotificationService;
import backend.xxx.chat.user.service.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(CommunityServiceCacheTest.CacheTestConfiguration.class)
class CommunityServiceCacheTest {

    private final CommunityService communityService;
    private final CommunityCategoryRepository communityCategoryRepository;
    private final CommunityTagRepository communityTagRepository;
    private final CacheManager cacheManager;

    @Autowired
    CommunityServiceCacheTest(
            CommunityService communityService,
            CommunityCategoryRepository communityCategoryRepository,
            CommunityTagRepository communityTagRepository,
            CacheManager cacheManager
    ) {
        this.communityService = communityService;
        this.communityCategoryRepository = communityCategoryRepository;
        this.communityTagRepository = communityTagRepository;
        this.cacheManager = cacheManager;
    }

    @BeforeEach
    void resetMocksAndCaches() {
        reset(communityCategoryRepository, communityTagRepository);
        cacheManager.getCache(RedisConfig.COMMUNITY_CATEGORIES_CACHE).clear();
        cacheManager.getCache(RedisConfig.COMMUNITY_TAGS_CACHE).clear();
    }

    @Test
    void cachesCategories() {
        CommunityCategory category = new CommunityCategory("trading", "Trading", "Trading rooms", 10, true);
        category.setId(1L);
        when(communityCategoryRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(category));

        List<CommunityCategoryResponse> firstResponse = communityService.getCategories();
        List<CommunityCategoryResponse> secondResponse = communityService.getCategories();

        assertThat(secondResponse).isSameAs(firstResponse);
        assertThat(secondResponse).extracting(CommunityCategoryResponse::slug).containsExactly("trading");
        verify(communityCategoryRepository, times(1)).findAllByActiveTrueOrderBySortOrderAscIdAsc();
    }

    @Test
    void cachesTags() {
        CommunityTag tag = new CommunityTag("btc", "BTC", true);
        tag.setId(1L);
        when(communityTagRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(tag));

        List<CommunityTagResponse> firstResponse = communityService.getTags();
        List<CommunityTagResponse> secondResponse = communityService.getTags();

        assertThat(secondResponse).isSameAs(firstResponse);
        assertThat(secondResponse).extracting(CommunityTagResponse::slug).containsExactly("btc");
        verify(communityTagRepository, times(1)).findAllByActiveTrueOrderByNameAsc();
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    RedisConfig.COMMUNITY_CATEGORIES_CACHE,
                    RedisConfig.COMMUNITY_TAGS_CACHE,
                    RedisConfig.COMMUNITY_DISCOVERY_CACHE,
                    RedisConfig.COMMUNITY_DETAIL_CACHE
            );
        }

        @Bean
        CommunityService communityService(
                UserLookupService userLookupService,
                CommunityCategoryRepository communityCategoryRepository,
                CommunityRepository communityRepository,
                CommunityTagRepository communityTagRepository,
                CommunityTagLinkRepository communityTagLinkRepository,
                CommunityMemberRepository communityMemberRepository,
                CommunityChannelRepository communityChannelRepository,
                ConversationRepository conversationRepository,
                ConversationParticipantRepository conversationParticipantRepository,
                CommunityValidator communityValidator,
                CommunityAccessPolicy communityAccessPolicy,
                CommunityAssetResolver communityAssetResolver,
                CommunityResponseBuilder communityResponseBuilder,
                CommunityMapper communityMapper,
                NotificationService notificationService
        ) {
            return new CommunityService(
                    userLookupService,
                    communityCategoryRepository,
                    communityRepository,
                    communityTagRepository,
                    communityTagLinkRepository,
                    communityMemberRepository,
                    communityChannelRepository,
                    conversationRepository,
                    conversationParticipantRepository,
                    communityValidator,
                    communityAccessPolicy,
                    communityAssetResolver,
                    communityResponseBuilder,
                    communityMapper,
                    notificationService
            );
        }

        @Bean
        CommunityMapper communityMapper() {
            return new CommunityMapper();
        }

        @Bean
        UserLookupService userLookupService() {
            return mock(UserLookupService.class);
        }

        @Bean
        CommunityCategoryRepository communityCategoryRepository() {
            return mock(CommunityCategoryRepository.class);
        }

        @Bean
        CommunityRepository communityRepository() {
            return mock(CommunityRepository.class);
        }

        @Bean
        CommunityTagRepository communityTagRepository() {
            return mock(CommunityTagRepository.class);
        }

        @Bean
        CommunityTagLinkRepository communityTagLinkRepository() {
            return mock(CommunityTagLinkRepository.class);
        }

        @Bean
        CommunityMemberRepository communityMemberRepository() {
            return mock(CommunityMemberRepository.class);
        }

        @Bean
        CommunityChannelRepository communityChannelRepository() {
            return mock(CommunityChannelRepository.class);
        }

        @Bean
        ConversationRepository conversationRepository() {
            return mock(ConversationRepository.class);
        }

        @Bean
        ConversationParticipantRepository conversationParticipantRepository() {
            return mock(ConversationParticipantRepository.class);
        }

        @Bean
        CommunityValidator communityValidator() {
            return mock(CommunityValidator.class);
        }

        @Bean
        CommunityAccessPolicy communityAccessPolicy() {
            return mock(CommunityAccessPolicy.class);
        }

        @Bean
        CommunityAssetResolver communityAssetResolver() {
            return mock(CommunityAssetResolver.class);
        }

        @Bean
        CommunityResponseBuilder communityResponseBuilder() {
            return mock(CommunityResponseBuilder.class);
        }

        @Bean
        NotificationService notificationService() {
            return mock(NotificationService.class);
        }
    }
}
