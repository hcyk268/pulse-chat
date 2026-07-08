package backend.xxx.chat.message.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
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

    @Column(name = "content", length = CONTENT_MAX_LENGTH)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

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

    @Column(name = "edited_at")
    private Instant editedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    private final List<MessageAttachment> attachments = new ArrayList<>();

    public static Message createTextMessage(
            Conversation conversation,
            User sender,
            UUID clientMessageId,
            String content
    ) {
        return createTextMessage(conversation, sender, clientMessageId, content, null);
    }

    public static Message createTextMessage(
            Conversation conversation,
            User sender,
            UUID clientMessageId,
            String content,
            Message replyToMessage
    ) {
        Message message = new Message();
        message.conversation = Objects.requireNonNull(conversation, "conversation must not be null");
        message.sender = Objects.requireNonNull(sender, "sender must not be null");
        message.clientMessageId = Objects.requireNonNull(clientMessageId, "clientMessageId must not be null");
        message.content = requireContent(content);
        message.replyToMessage = replyToMessage;
        message.messageType = MessageType.TEXT;
        message.status = MessageStatus.SENT;
        return message;
    }

    public static Message createMediaMessage(
            Conversation conversation,
            User sender,
            UUID clientMessageId,
            String content,
            Message replyToMessage
    ) {
        Message message = new Message();
        message.conversation = Objects.requireNonNull(conversation, "conversation must not be null");
        message.sender = Objects.requireNonNull(sender, "sender must not be null");
        message.clientMessageId = Objects.requireNonNull(clientMessageId, "clientMessageId must not be null");
        message.content = normalizeOptionalContent(content);
        message.replyToMessage = replyToMessage;
        message.messageType = MessageType.MEDIA;
        message.status = MessageStatus.SENT;
        return message;
    }

    public void addAttachment(MessageAttachment attachment) {
        ensureNotDeleted();
        MessageAttachment requiredAttachment = Objects.requireNonNull(attachment, "attachment must not be null");
        requiredAttachment.attachTo(this);
        attachments.add(requiredAttachment);
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

    public void editContent(String content, Instant editedAt) {
        ensureNotDeleted();
        this.content = requireContent(content);
        this.editedAt = Objects.requireNonNull(editedAt, "editedAt must not be null");
    }

    public void deleteForEveryone(User deletedBy, Instant deletedAt) {
        this.deletedBy = Objects.requireNonNull(deletedBy, "deletedBy must not be null");
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    }

    public boolean isDeleted() {
        return deletedAt != null;
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

    private static String normalizeOptionalContent(String content) {
        if (content == null) {
            return null;
        }

        String trimmedContent = content.trim();
        if (trimmedContent.isEmpty()) {
            return null;
        }

        if (trimmedContent.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("content exceeds max length " + CONTENT_MAX_LENGTH);
        }

        return trimmedContent;
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException("Deleted message cannot be changed");
        }
    }
}
