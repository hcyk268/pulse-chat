package backend.xxx.chat.message.model;

import java.time.Instant;
import java.util.Objects;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "message_pins",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_message_pins_conversation_message",
                        columnNames = {"conversation_id", "message_id"}
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class MessagePin extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pinned_by", nullable = false)
    private User pinnedBy;

    @Column(name = "pinned_at", nullable = false)
    private Instant pinnedAt;

    public static MessagePin create(
            Conversation conversation,
            Message message,
            User pinnedBy,
            Instant pinnedAt
    ) {
        MessagePin messagePin = new MessagePin();
        messagePin.conversation = Objects.requireNonNull(conversation, "conversation must not be null");
        messagePin.message = Objects.requireNonNull(message, "message must not be null");
        messagePin.pinnedBy = Objects.requireNonNull(pinnedBy, "pinnedBy must not be null");
        messagePin.pinnedAt = Objects.requireNonNull(pinnedAt, "pinnedAt must not be null");
        return messagePin;
    }

    @PrePersist
    void prePersist() {
        if (pinnedAt == null) {
            pinnedAt = Instant.now();
        }
    }
}
