package backend.xxx.chat.conversation.service;

import java.util.List;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.config.RedisConfig;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationParticipantCacheService {

    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationRepository conversationRepository;
    private final CacheManager cacheManager;

    @Cacheable(cacheNames = RedisConfig.CACHED_CONVERSATION_PARTICIPANTS_CACHE, key = "#conversationId")
    public List<CachedConversationParticipant> getParticipants(Long conversationId) {
        List<CachedConversationParticipant> participants = conversationParticipantRepository
                .findByConversationIdWithUser(conversationId)
                .stream()
                .map(CachedConversationParticipant::from)
                .toList();

        if (participants.isEmpty() && !conversationRepository.existsById(conversationId)) {
            throw new NotFoundException("conversation.not.found");
        }

        return participants;
    }

    public void evictParticipants(Long conversationId) {
        Cache cache = cacheManager.getCache(RedisConfig.CACHED_CONVERSATION_PARTICIPANTS_CACHE);
        if (cache == null || conversationId == null) {
            return;
        }

        cache.evict(conversationId);
    }
}
