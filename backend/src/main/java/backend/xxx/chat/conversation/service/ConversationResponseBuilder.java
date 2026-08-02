package backend.xxx.chat.conversation.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import backend.xxx.chat.conversation.dto.ConversationDetailResponse;
import backend.xxx.chat.conversation.dto.ConversationResponse;
import backend.xxx.chat.conversation.dto.DirectConversationResponse;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageAttachment;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.message.repository.MessageAttachmentRepository;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.PresenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationResponseBuilder {

    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final PresenceRepository presenceRepository;
    private final ConversationMapper conversationMapper;

    public List<ConversationResponse> buildForCurrentUser(
            List<ConversationParticipant> currentParticipants,
            User currentUser
    ) {
        if (currentParticipants.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ConversationParticipant>> participantsByConversationId =
                findParticipantsByConversationId(currentParticipants);
        Map<Long, Presence> presenceByUserId = findPresenceByUserId(
                participantsByConversationId.values().stream()
                        .flatMap(List::stream)
                        .toList()
        );
        Map<Long, Message> lastMessageById = findLastMessageById(currentParticipants);
        Map<Long, List<MessageAttachment>> attachmentsByMessageId = findAttachmentsByMessageId(lastMessageById.values());

        return currentParticipants.stream()
                .map(currentParticipant -> {
                    Conversation conversation = currentParticipant.getConversation();
                    List<ConversationParticipant> participants = participantsByConversationId.getOrDefault(
                            conversation.getId(),
                            List.of()
                    );

                    return conversationMapper.toConversationResponse(
                            currentParticipant,
                            currentUser,
                            participants,
                            presenceByUserId,
                            lastMessageById,
                            attachmentsByMessageId
                    );
                })
                .toList();
    }

    public Map<String, ConversationResponse> buildByUsernameForParticipants(
            List<ConversationParticipant> participants,
            Map<Long, Message> lastMessageById,
            Map<Long, List<MessageAttachment>> attachmentsByMessageId
    ) {
        if (participants.isEmpty()) {
            return Map.of();
        }

        Map<Long, Presence> presenceByUserId = findPresenceByUserId(participants);

        return participants.stream()
                .collect(Collectors.toMap(
                        participant -> participant.getUser().getUsername(),
                        participant -> conversationMapper.toConversationResponse(
                                participant,
                                participant.getUser(),
                                participants,
                                presenceByUserId,
                                lastMessageById,
                                attachmentsByMessageId
                        ),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    public ConversationDetailResponse buildConversationDetailResponse(
            Conversation conversation,
            User currentUser,
            List<ConversationParticipant> participants
    ) {
        Map<Long, Presence> presenceByUserId = findPresenceByUserId(participants);
        Message lastMessage = findLastMessage(conversation);
        Map<Long, List<MessageAttachment>> attachmentsByMessageId = findAttachmentsByMessageId(singleMessage(lastMessage));

        return conversationMapper.toConversationDetailResponse(
                conversation,
                currentUser,
                participants,
                presenceByUserId,
                lastMessage,
                attachmentsByMessageId
        );
    }

    public ConversationDetailResponse buildCachedConversationDetailResponse(
            Conversation conversation,
            User currentUser,
            List<CachedConversationParticipant> participants
    ) {
        Map<Long, Presence> presenceByUserId = findPresenceByCachedUserId(participants);
        Message lastMessage = findLastMessage(conversation);
        Map<Long, List<MessageAttachment>> attachmentsByMessageId = findAttachmentsByMessageId(singleMessage(lastMessage));

        return conversationMapper.toCachedConversationDetailResponse(
                conversation,
                currentUser,
                participants,
                presenceByUserId,
                lastMessage,
                attachmentsByMessageId
        );
    }
    public DirectConversationResponse buildDirectConversationResponse(
            Conversation conversation,
            User currentUser,
            User targetUser
    ) {
        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversationIdWithUser(conversation.getId());
        Map<Long, Presence> presenceByUserId = findPresenceByUserId(participants);
        Message lastMessage = findLastMessage(conversation);
        Map<Long, List<MessageAttachment>> attachmentsByMessageId = findAttachmentsByMessageId(singleMessage(lastMessage));

        return conversationMapper.toDirectConversationResponse(
                conversation,
                participants,
                currentUser,
                targetUser,
                presenceByUserId,
                lastMessage,
                attachmentsByMessageId
        );
    }

    private Map<Long, List<ConversationParticipant>> findParticipantsByConversationId(
            List<ConversationParticipant> currentParticipants
    ) {
        List<Long> conversationIds = currentParticipants.stream()
                .map(participant -> participant.getConversation().getId())
                .distinct()
                .toList();

        if (conversationIds.isEmpty()) {
            return Map.of();
        }

        return conversationParticipantRepository.findByConversationIdInWithUser(conversationIds)
                .stream()
                .collect(Collectors.groupingBy(participant -> participant.getConversation().getId()));
    }

    private Map<Long, Presence> findPresenceByCachedUserId(List<CachedConversationParticipant> participants) {
        List<Long> userIds = participants.stream()
                .map(CachedConversationParticipant::userId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return presenceRepository.findByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(Presence::getUserId, Function.identity()));
    }
    private Map<Long, Presence> findPresenceByUserId(List<ConversationParticipant> participants) {
        List<Long> userIds = participants.stream()
                .map(participant -> participant.getUser().getId())
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return presenceRepository.findByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(Presence::getUserId, Function.identity()));
    }

    private Map<Long, Message> findLastMessageById(List<ConversationParticipant> participants) {
        List<Long> lastMessageIds = participants.stream()
                .map(participant -> participant.getConversation().getLastMessageId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (lastMessageIds.isEmpty()) {
            return Map.of();
        }

        return messageRepository.findByIdInWithSender(lastMessageIds)
                .stream()
                .collect(Collectors.toMap(Message::getId, Function.identity()));
    }

    private Map<Long, List<MessageAttachment>> findAttachmentsByMessageId(Collection<Message> messages) {
        List<Long> messageIds = messages.stream()
                .filter(this::needsAttachmentPreview)
                .map(Message::getId)
                .distinct()
                .toList();

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

    private boolean needsAttachmentPreview(Message message) {
        if (message == null || message.isDeleted() || message.getId() == null) {
            return false;
        }

        if (message.getMessageType() != MessageType.MEDIA) {
            return false;
        }

        String content = message.getContent();
        return content == null || content.isBlank();
    }

    private List<Message> singleMessage(Message message) {
        return message == null ? List.of() : List.of(message);
    }

    private Message findLastMessage(Conversation conversation) {
        if (conversation.getLastMessageId() == null) {
            return null;
        }

        return messageRepository.findByIdInWithSender(List.of(conversation.getLastMessageId()))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
