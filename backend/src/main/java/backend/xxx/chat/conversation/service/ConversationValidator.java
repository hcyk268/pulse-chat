package backend.xxx.chat.conversation.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.dto.ConversationCursor;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ParticipantRole;
import backend.xxx.chat.user.model.AccountStatus;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class ConversationValidator {

    public void validateDirectConversationTarget(User currentUser, User targetUser) {
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new ValidationException("targetUserId must not be the current user");
        }

        if (targetUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("Target user is not active");
        }
    }

    public Set<Long> validateMemberIds(
            Long currentUserId,
            List<Long> memberIds,
            int minDistinctMembers
    ) {
        Set<Long> distinctMemberIds = new LinkedHashSet<>(memberIds == null ? List.of() : memberIds);
        if (distinctMemberIds.contains(currentUserId)) {
            throw new ValidationException("memberIds must not contain the current user");
        }

        if (distinctMemberIds.size() < minDistinctMembers) {
            throw new ValidationException("memberIds must contain at least " + minDistinctMembers + " distinct users");
        }

        return distinctMemberIds;
    }

    public void validateResolvedInvitees(Set<Long> memberIds, List<User> users) {
        if (users.size() != memberIds.size()) {
            throw new NotFoundException("One or more users not found");
        }

        users.forEach(user -> {
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new ValidationException("All group members must be active users");
            }
        });
    }

    public void validateCanInviteParticipant(ConversationParticipant participant) {
        if (participant != null && (participant.isActive() || participant.isPending())) {
            throw new ConflictException("User is already a member or has a pending invitation");
        }
    }

    public void validatePendingInvitation(ConversationParticipant participant) {
        if (!participant.isPending()) {
            throw new ConflictException("No pending group invitation found");
        }
    }

    public void validateOwnerCannotRemoveSelf(Long currentUserId, Long memberId) {
        if (currentUserId.equals(memberId)) {
            throw new ValidationException("Owner must use leave group instead of remove member");
        }
    }

    public void validateMemberNotLeft(ConversationParticipant participant) {
        if (participant.isLeft()) {
            throw new ConflictException("Member already left this group");
        }
    }

    public void validateRoleChange(
            ParticipantRole currentRole,
            ParticipantRole newRole,
            boolean hasAnotherActiveOwner
    ) {
        if (currentRole == ParticipantRole.OWNER
                && newRole == ParticipantRole.MEMBER
                && !hasAnotherActiveOwner) {
            throw new ConflictException("Group must have at least one owner");
        }
    }

    public String normalizeRequiredGroupName(String name) {
        String normalized = normalizeOptionalText(name);
        if (normalized == null) {
            throw new ValidationException("Group name must not be blank");
        }
        return normalized;
    }

    public String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public void validateConversationCursor(ConversationCursor conversationCursor) {
        if (conversationCursor != null
                && (conversationCursor.cursorAt() == null || conversationCursor.conversationId() == null)) {
            throw new ValidationException("Invalid conversation cursor");
        }
    }

    public int normalizeConversationLimit(Short limit, int defaultLimit, int maxLimit) {
        int pageLimit = limit == null ? defaultLimit : limit;

        if (pageLimit < 1 || pageLimit > maxLimit) {
            throw new ValidationException("limit must be between 1 and " + maxLimit);
        }

        return pageLimit;
    }
}