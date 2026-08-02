package backend.xxx.chat.conversation.service;

import java.time.Instant;

import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ParticipantRole;
import backend.xxx.chat.conversation.model.ParticipantStatus;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;

public record CachedConversationParticipant(
        Long conversationId,
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        ParticipantRole role,
        ParticipantStatus status,
        Instant joinedAt,
        Long lastReadMessageId,
        long unreadCount,
        boolean visibleInList,
        Instant leftAt
) {

    public static CachedConversationParticipant from(ConversationParticipant participant) {
        User user = participant.getUser();
        return new CachedConversationParticipant(
                participant.getConversation().getId(),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                participant.getRole(),
                participant.getStatus(),
                participant.getJoinedAt(),
                participant.getLastReadMessageId(),
                participant.getUnreadCount(),
                participant.isVisibleInList(),
                participant.getLeftAt()
        );
    }

    public boolean active() {
        return status == ParticipantStatus.ACTIVE && leftAt == null;
    }

    public boolean left() {
        return status == ParticipantStatus.LEFT || leftAt != null;
    }

    public Presence presencePlaceholder() {
        return null;
    }
}
