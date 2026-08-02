package backend.xxx.chat.message.service;

import java.util.List;

import backend.xxx.chat.config.RedisConfig;
import backend.xxx.chat.conversation.dto.ConversationPinnedMessagesResponse;
import backend.xxx.chat.message.repository.MessageAttachmentRepository;
import backend.xxx.chat.message.repository.MessagePinRepository;
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

@SpringJUnitConfig(MessagePinCacheServiceTest.CacheTestConfiguration.class)
class MessagePinCacheServiceTest {

    private final MessagePinCacheService messagePinCacheService;
    private final MessagePinRepository messagePinRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final CacheManager cacheManager;

    @Autowired
    MessagePinCacheServiceTest(
            MessagePinCacheService messagePinCacheService,
            MessagePinRepository messagePinRepository,
            MessageAttachmentRepository messageAttachmentRepository,
            CacheManager cacheManager
    ) {
        this.messagePinCacheService = messagePinCacheService;
        this.messagePinRepository = messagePinRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.cacheManager = cacheManager;
    }

    @BeforeEach
    void resetMocksAndCache() {
        reset(messagePinRepository, messageAttachmentRepository);
        cacheManager.getCache(RedisConfig.CONVERSATION_PINNED_MESSAGES_CACHE).clear();
    }

    @Test
    void cachesPinnedMessagesByConversationId() {
        when(messagePinRepository.findByConversationIdWithDetails(1L)).thenReturn(List.of());

        ConversationPinnedMessagesResponse firstResponse = messagePinCacheService.getPinnedMessages(1L);
        ConversationPinnedMessagesResponse secondResponse = messagePinCacheService.getPinnedMessages(1L);

        assertThat(secondResponse).isSameAs(firstResponse);
        assertThat(secondResponse.conversationId()).isEqualTo(1L);
        assertThat(secondResponse.items()).isEmpty();
        verify(messagePinRepository, times(1)).findByConversationIdWithDetails(1L);
    }

    @Test
    void evictsPinnedMessagesByConversationId() {
        when(messagePinRepository.findByConversationIdWithDetails(1L)).thenReturn(List.of());

        messagePinCacheService.getPinnedMessages(1L);
        messagePinCacheService.evictPinnedMessages(1L);
        messagePinCacheService.getPinnedMessages(1L);

        verify(messagePinRepository, times(2)).findByConversationIdWithDetails(1L);
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(RedisConfig.CONVERSATION_PINNED_MESSAGES_CACHE);
        }

        @Bean
        MessagePinCacheService messagePinCacheService(
                MessagePinRepository messagePinRepository,
                MessageAttachmentRepository messageAttachmentRepository,
                MessagePinMapper messagePinMapper,
                CacheManager cacheManager
        ) {
            return new MessagePinCacheService(
                    messagePinRepository,
                    messageAttachmentRepository,
                    messagePinMapper,
                    cacheManager
            );
        }

        @Bean
        MessagePinRepository messagePinRepository() {
            return mock(MessagePinRepository.class);
        }

        @Bean
        MessageAttachmentRepository messageAttachmentRepository() {
            return mock(MessageAttachmentRepository.class);
        }

        @Bean
        MessagePinMapper messagePinMapper() {
            return mock(MessagePinMapper.class);
        }
    }
}