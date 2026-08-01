package backend.xxx.chat.message.model;

import java.time.Instant;
import java.util.Objects;

import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Getter
@Entity
@Table(name = "message_reads")
@NoArgsConstructor
@AllArgsConstructor
public class MessageRead implements Persistable<MessageReadId> {

    @EmbeddedId
    private MessageReadId id;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("messageId")
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @Override
    public MessageReadId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
    }

    public static MessageRead create(Message message, User user, Instant readAt) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(user, "user must not be null");

        MessageRead read = new MessageRead();
        read.id = new MessageReadId(message.getId(), user.getId());
        read.message = message;
        read.user = user;
        read.readAt = Objects.requireNonNull(readAt, "readAt must not be null");
        return read;
    }
}