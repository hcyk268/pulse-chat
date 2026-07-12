package backend.xxx.chat.conversation.service;

import java.util.List;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ForbiddenException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.model.*;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationAccessPolicy {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;

    public ConversationParticipant requireOwner(Long conversationId, Long userId) {
        ConversationParticipant participant = requireActiveMember(conversationId, userId);
        if (participant.getRole() != ParticipantRole.OWNER) {
            throw new ForbiddenException("Only group owner can perform this action");
        }
        return participant;
    }

    public ConversationParticipant requireActiveMember(Long conversationId, Long userId) {
        ConversationParticipant participant = requireParticipant(conversationId, userId);
        if (!participant.isActive()) {
            throw new ForbiddenException("You are not an active member of this group");
        }
        return participant;
    }

    public Conversation requireGroupConversation(Long conversationId) {
        Conversation conversation = requireConversation(conversationId);

        if (conversation.getType() != ConversationType.GROUP) {
            throw new ValidationException("Conversation is not a group");
        }

        return conversation;
    }

    public Conversation requireConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    public List<ConversationParticipant> requireParticipants(Long conversationId) {
        validateConversationId(conversationId);

        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversationIdWithUser(conversationId);

        if (participants.isEmpty() && !conversationRepository.existsById(conversationId)) {
            throw new NotFoundException("Conversation not found");
        }

        return participants;
    }

    public ConversationParticipant requireParticipant(Long conversationId, Long userId) {
        validateConversationId(conversationId);
        if (userId == null) {
            throw new ValidationException("userId must not be null");
        }

        return conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversationId, userId)
                )
                .orElseThrow(() -> {
                    if (!conversationRepository.existsById(conversationId)) {
                        return new NotFoundException("Conversation not found");
                    }
                    return new ForbiddenException("You are not allowed to access this conversation");
                });
    }

    public ConversationParticipant requireActiveParticipant(Long conversationId, Long userId) {
        ConversationParticipant participant = requireParticipant(conversationId, userId);

        if (!participant.isActive()) {
            throw new ForbiddenException("You are not allowed to access this conversation");
        }

        return participant;
    }

    public void assertCanSendMessage(User user, List<ConversationParticipant> participants) {
        if (!isParticipant(user, participants)) {
            throw new ForbiddenException("You are not allowed to send message to this conversation");
        }
    }

    public void assertCanReadConversation(User user, List<ConversationParticipant> participants) {
        if (!isParticipant(user, participants)) {
            throw new ForbiddenException("You are not allowed to access this conversation");
        }
    }

    public void assertCanUpdateTyping(User user, List<ConversationParticipant> participants) {
        if (!isParticipant(user, participants)) {
            throw new ForbiddenException("You are not allowed to update typing status for this conversation");
        }
    }

    public void assertCanUpdateMessageStatus(User user, List<ConversationParticipant> participants) {
        if (!isParticipant(user, participants)) {
            throw new ForbiddenException("You are not allowed to update this message status");
        }
    }

    private boolean isParticipant(User user, List<ConversationParticipant> participants) {
        if (user == null || user.getId() == null || participants == null) {
            return false;
        }

        return participants.stream()
                .anyMatch(participant -> participant.isActive()
                        && participant.getUser().getId().equals(user.getId()));
    }

    private void validateConversationId(Long conversationId) {
        if (conversationId == null) {
            throw new ValidationException("conversationId must not be null");
        }
    }
}
