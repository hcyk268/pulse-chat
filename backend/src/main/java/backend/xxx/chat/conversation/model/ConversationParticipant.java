package backend.xxx.chat.conversation.model;

import java.time.Instant;

import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
        return participant;
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
