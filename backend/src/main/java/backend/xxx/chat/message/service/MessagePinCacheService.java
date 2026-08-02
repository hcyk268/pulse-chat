package backend.xxx.chat.message.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import backend.xxx.chat.config.RedisConfig;
import backend.xxx.chat.conversation.dto.ConversationPinnedMessagesResponse;
import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageAttachment;
import backend.xxx.chat.message.model.MessagePin;
import backend.xxx.chat.message.repository.MessageAttachmentRepository;
import backend.xxx.chat.message.repository.MessagePinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessagePinCacheService {

    private final MessagePinRepository messagePinRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessagePinMapper messagePinMapper;
    private final CacheManager cacheManager;

    @Cacheable(cacheNames = RedisConfig.CONVERSATION_PINNED_MESSAGES_CACHE, key = "#conversationId")
    public ConversationPinnedMessagesResponse getPinnedMessages(Long conversationId) {
        List<MessagePin> messagePins = messagePinRepository.findByConversationIdWithDetails(conversationId);
        Map<Long, List<MessageAttachment>> attachmentsByMessageId = findAttachmentsByMessageId(
                collectMessageAndReplyIds(messagePins.stream()
                        .map(MessagePin::getMessage)
                        .toList())
        );

        List<MessagePinResponse> items = messagePins.stream()
                .map(messagePin -> messagePinMapper.toResponse(messagePin, attachmentsByMessageId))
                .toList();

        return new ConversationPinnedMessagesResponse(conversationId, items);
    }

    public void evictPinnedMessages(Long conversationId) {
        Cache cache = cacheManager.getCache(RedisConfig.CONVERSATION_PINNED_MESSAGES_CACHE);
        if (cache != null) {
            cache.evict(conversationId);
        }
    }

    private Set<Long> collectMessageAndReplyIds(Collection<Message> messages) {
        Set<Long> messageIds = messages.stream()
                .map(Message::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        messages.stream()
                .map(Message::getReplyToMessage)
                .filter(Objects::nonNull)
                .map(Message::getId)
                .forEach(messageIds::add);

        return messageIds;
    }

    private Map<Long, List<MessageAttachment>> findAttachmentsByMessageId(Collection<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }

        return messageAttachmentRepository.findByMessageIdInWithUploadedAsset(messageIds)
                .stream()
                .collect(Collectors.groupingBy(
                        attachment -> attachment.getMessage().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}