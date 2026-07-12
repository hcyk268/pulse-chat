package backend.xxx.chat.conversation.model;

import java.time.Instant;

import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "conversation_participants")
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {

    @EmbeddedId
    private ConversationParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "unread_count", nullable = false)
    private long unreadCount;

    @Column(name = "is_visible_in_list", nullable = false)
    private boolean isVisibleInList;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ParticipantRole role = ParticipantRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ParticipantStatus status = ParticipantStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "added_by")
    private User addedBy;

    @Column(name = "left_at")
    private Instant leftAt;

    public static ConversationParticipant create(Conversation conversation, User user) {
        return create(conversation, user, false);
    }

    public static ConversationParticipant create(
            Conversation conversation,
            User user,
            boolean isVisibleInList
    ) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.id = new ConversationParticipantId(conversation.getId(), user.getId());
        participant.conversation = conversation;
        participant.user = user;
        participant.unreadCount = 0L;
        participant.isVisibleInList = isVisibleInList;
        participant.role = ParticipantRole.MEMBER;
        participant.status = ParticipantStatus.ACTIVE;
        return participant;
    }

    public static ConversationParticipant createPending(
            Conversation conversation,
            User user,
            User addedBy
    ) {
        ConversationParticipant participant = create(conversation, user, true);
        participant.status = ParticipantStatus.PENDING;
        participant.addedBy = addedBy;
        return participant;
    }

    public void promoteToOwner() {
        this.role = ParticipantRole.OWNER;
    }

    public void changeRole(ParticipantRole role) {
        this.role = role;
    }

    public void acceptInvitation() {
        this.status = ParticipantStatus.ACTIVE;
        this.leftAt = null;
        markVisibleInList();
    }

    public void markPendingInvitation(User addedBy) {
        this.status = ParticipantStatus.PENDING;
        this.role = ParticipantRole.MEMBER;
        this.addedBy = addedBy;
        this.leftAt = null;
        markVisibleInList();
    }

    public void markLeft(Instant leftAt) {
        this.status = ParticipantStatus.LEFT;
        this.leftAt = leftAt;
    }

    public boolean isLeft() {
        return status == ParticipantStatus.LEFT || leftAt != null;
    }

    public boolean isActive() {
        return status == ParticipantStatus.ACTIVE && leftAt == null;
    }

    public boolean isPending() {
        return status == ParticipantStatus.PENDING;
    }

    public void markRead(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
        this.unreadCount = 0L;
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    public void markVisibleInList() {
        this.isVisibleInList = true;
    }

    public void hideFromList() {
        this.isVisibleInList = false;
    }
}