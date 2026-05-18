package backend.xxx.chat.conversation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import backend.xxx.chat.conversation.dto.ConversationResponse;
import backend.xxx.chat.conversation.dto.DirectConversationResponse;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.message.model.Message;
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
                            lastMessageById
                    );
                })
                .toList();
    }

    public Map<String, ConversationResponse> buildByUsernameForParticipants(
            List<ConversationParticipant> participants
    ) {
        if (participants.isEmpty()) {
            return Map.of();
        }

        Map<Long, Presence> presenceByUserId = findPresenceByUserId(participants);
        Map<Long, Message> lastMessageById = findLastMessageById(participants);

        return participants.stream()
                .collect(Collectors.toMap(
                        participant -> participant.getUser().getUsername(),
                        participant -> conversationMapper.toConversationResponse(
                                participant,
                                participant.getUser(),
                                participants,
                                presenceByUserId,
                                lastMessageById
                        ),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
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

        return conversationMapper.toDirectConversationResponse(
                conversation,
                participants,
                currentUser,
                targetUser,
                presenceByUserId,
                lastMessage
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
