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
    private final ConversationParticipantCacheService conversationParticipantCacheService;

    public ConversationParticipant requireOwner(Long conversationId, Long userId) {
        ConversationParticipant participant = requireActiveMember(conversationId, userId);
        if (participant.getRole() != ParticipantRole.OWNER) {
            throw new ForbiddenException("conversation.owner.only");
        }
        return participant;
    }

    public ConversationParticipant requireActiveMember(Long conversationId, Long userId) {
        ConversationParticipant participant = requireParticipant(conversationId, userId);
        if (!participant.isActive()) {
            throw new ForbiddenException("conversation.active.member.required");
        }
        return participant;
    }

    public Conversation requireGroupConversation(Long conversationId) {
        Conversation conversation = requireConversation(conversationId);

        if (conversation.getType() != ConversationType.GROUP) {
            throw new ValidationException("conversation.not.group");
        }

        return conversation;
    }

    public Conversation requireConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("conversation.not.found"));
    }

    public List<ConversationParticipant> requireParticipants(Long conversationId) {
        validateConversationId(conversationId);

        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversationIdWithUser(conversationId);

        if (participants.isEmpty() && !conversationRepository.existsById(conversationId)) {
            throw new NotFoundException("conversation.not.found");
        }

        return participants;
    }

    public List<CachedConversationParticipant> requireParticipantSnapshots(Long conversationId) {
        validateConversationId(conversationId);
        return conversationParticipantCacheService.getParticipants(conversationId);
    }

    public ConversationParticipant requireParticipant(Long conversationId, Long userId) {
        validateConversationId(conversationId);
        validateUserId(userId);

        return conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversationId, userId)
                )
                .orElseThrow(() -> {
                    if (!conversationRepository.existsById(conversationId)) {
                        return new NotFoundException("conversation.not.found");
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

    public void assertCanSendMessage(Long conversationId, Long userId) {
        requireActiveMembership(conversationId, userId, "You are not allowed to send message to this conversation");
    }

    public void assertCanReadConversation(Long conversationId, Long userId) {
        requireActiveMembership(conversationId, userId, "You are not allowed to access this conversation");
    }

    public void assertCanUpdateTyping(Long conversationId, Long userId) {
        requireActiveMembership(conversationId, userId, "You are not allowed to update typing status for this conversation");
    }

    public void assertCanUpdateMessageStatus(Long conversationId, Long userId) {
        requireActiveMembership(conversationId, userId, "You are not allowed to update this message status");
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

    public List<ConversationParticipant> filterActiveParticipants(List<ConversationParticipant> participants) {
        if (participants == null) {
            return List.of();
        }

        return participants.stream()
                .filter(ConversationParticipant::isActive)
                .toList();
    }

    private void requireActiveMembership(Long conversationId, Long userId, String forbiddenMessage) {
        validateConversationId(conversationId);
        validateUserId(userId);

        boolean active = conversationParticipantRepository
                .existsByConversationIdAndUserIdAndStatusAndLeftAtIsNull(
                        conversationId,
                        userId,
                        ParticipantStatus.ACTIVE
                );
        if (active) {
            return;
        }

        if (!conversationRepository.existsById(conversationId)) {
            throw new NotFoundException("conversation.not.found");
        }

        throw new ForbiddenException(forbiddenMessage);
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

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new ValidationException("user.id.required");
        }
    }
}