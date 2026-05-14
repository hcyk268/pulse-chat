package backend.xxx.chat.message.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "messages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_messages_conversation_client_message_id",
                        columnNames = {"conversation_id", "client_message_id"}
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class Message extends AbstractBaseEntity<Long> {

    private static final int CONTENT_MAX_LENGTH = 4000;

    @Column(name = "client_message_id", nullable = false)
    private UUID clientMessageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    public static Message createTextMessage(
            Conversation conversation,
            User sender,
            UUID clientMessageId,
            String content
    ) {
        Message message = new Message();
        message.conversation = Objects.requireNonNull(conversation, "conversation must not be null");
        message.sender = Objects.requireNonNull(sender, "sender must not be null");
        message.clientMessageId = Objects.requireNonNull(clientMessageId, "clientMessageId must not be null");
        message.content = requireContent(content);
        message.messageType = MessageType.TEXT;
        message.status = MessageStatus.SENT;
        return message;
    }

    public void markDelivered(Instant deliveredAt) {
        if (this.status == MessageStatus.READ) {
            return;
        }
        this.status = MessageStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
    }

    public void markRead(Instant readAt) {
        this.status = MessageStatus.READ;
        this.readAt = readAt;
        if (this.deliveredAt == null) {
            this.deliveredAt = readAt;
        }
    }

    private static String requireContent(String content) {
        Objects.requireNonNull(content, "content must not be null");
        String trimmedContent = content.trim();

        if (trimmedContent.isEmpty()) {
            throw new IllegalArgumentException("content must not be blank");
        }

        if (trimmedContent.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("content exceeds max length " + CONTENT_MAX_LENGTH);
        }

        return trimmedContent;
    }
}
