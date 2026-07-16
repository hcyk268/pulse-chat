package backend.xxx.chat.conversation.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.web.Translator;
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
            throw new ValidationException("conversation.target.self.not.allowed");
        }

        if (targetUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("conversation.target.user.inactive");
        }
    }

    public Set<Long> validateMemberIds(
            Long currentUserId,
            List<Long> memberIds,
            int minDistinctMembers
    ) {
        Set<Long> distinctMemberIds = new LinkedHashSet<>(memberIds == null ? List.of() : memberIds);
        if (distinctMemberIds.contains(currentUserId)) {
            throw new ValidationException("conversation.member.ids.self.not.allowed");
        }

        if (distinctMemberIds.size() < minDistinctMembers) {
            throw new ValidationException(Translator.toLocale("conversation.member.ids.min.distinct", minDistinctMembers));
        }

        return distinctMemberIds;
    }

    public void validateResolvedInvitees(Set<Long> memberIds, List<User> users) {
        if (users.size() != memberIds.size()) {
            throw new NotFoundException("conversation.members.not.found");
        }

        users.forEach(user -> {
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new ValidationException("conversation.group.members.inactive");
            }
        });
    }

    public void validateCanInviteParticipant(ConversationParticipant participant) {
        if (participant != null && (participant.isActive() || participant.isPending())) {
            throw new ConflictException("conversation.group.invitation.exists");
        }
    }

    public void validatePendingInvitation(ConversationParticipant participant) {
        if (!participant.isPending()) {
            throw new ConflictException("conversation.group.invitation.not.found");
        }
    }

    public void validateOwnerCannotRemoveSelf(Long currentUserId, Long memberId) {
        if (currentUserId.equals(memberId)) {
            throw new ValidationException("conversation.group.leave.required");
        }
    }

    public void validateMemberNotLeft(ConversationParticipant participant) {
        if (participant.isLeft()) {
            throw new ConflictException("conversation.member.left.already");
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
            throw new ConflictException("conversation.owner.required");
        }
    }

    public String normalizeRequiredGroupName(String name) {
        String normalized = normalizeOptionalText(name);
        if (normalized == null) {
            throw new ValidationException("conversation.name.blank");
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
            throw new ValidationException("conversation.cursor.invalid");
        }
    }

    public int normalizeConversationLimit(Short limit, int defaultLimit, int maxLimit) {
        int pageLimit = limit == null ? defaultLimit : limit;

        if (pageLimit < 1 || pageLimit > maxLimit) {
            throw new ValidationException(Translator.toLocale("validation.limit.range", maxLimit));
        }

        return pageLimit;
    }
}