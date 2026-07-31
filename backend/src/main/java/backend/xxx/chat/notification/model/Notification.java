package backend.xxx.chat.notification.model;

import java.time.Instant;
import java.util.Objects;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.common.model.AbstractBaseEntity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor
public class Notification extends AbstractBaseEntity<Long> {

    private static final int TITLE_MAX_LENGTH = 150;
    private static final int BODY_MAX_LENGTH = 500;
    private static final int SOURCE_TYPE_MAX_LENGTH = 30;
    private static final int DEDUPE_KEY_MAX_LENGTH = 150;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "body", length = BODY_MAX_LENGTH)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30)
    private NotificationTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "source_type", length = SOURCE_TYPE_MAX_LENGTH)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "dedupe_key", length = DEDUPE_KEY_MAX_LENGTH)
    private String dedupeKey;

    @Column(name = "read_at")
    private Instant readAt;

    public static Notification create(
            User recipient,
            User actor,
            NotificationType type,
            String title,
            String body,
            NotificationTargetType targetType,
            Long targetId,
            String sourceType,
            Long sourceId,
            String dedupeKey
    ) {
        Notification notification = new Notification();
        notification.recipient = Objects.requireNonNull(recipient, "notification.recipient.required");
        notification.actor = actor;
        notification.type = Objects.requireNonNull(type, "notification.type.required");
        notification.title = requireText(title, "notification.title.required", TITLE_MAX_LENGTH);
        notification.body = normalizeText(body, BODY_MAX_LENGTH, "notification.body.max.length");
        notification.targetType = targetType;
        notification.targetId = targetId;
        notification.sourceType = normalizeText(
                sourceType,
                SOURCE_TYPE_MAX_LENGTH,
                "notification.source-type.max.length"
        );
        notification.sourceId = sourceId;
        notification.dedupeKey = normalizeText(
                dedupeKey,
                DEDUPE_KEY_MAX_LENGTH,
                "notification.dedupe-key.max.length"
        );
        notification.validateTarget();
        return notification;
    }

    public boolean markRead(Instant readAt) {
        if (this.readAt != null) {
            return false;
        }
        this.readAt = Objects.requireNonNull(readAt, "notification.read-at.required");
        return true;
    }

    public boolean markUnread() {
        if (readAt == null) {
            return false;
        }
        readAt = null;
        return true;
    }

    public boolean isRead() {
        return readAt != null;
    }

    private void validateTarget() {
        if ((targetType == null) != (targetId == null)) {
            throw new ValidationException("notification.target.invalid");
        }
        if (targetId != null && targetId <= 0) {
            throw new ValidationException("notification.target-id.positive");
        }
        if ((sourceType == null) != (sourceId == null)) {
            throw new ValidationException("notification.source.invalid");
        }
        if (sourceId != null && sourceId <= 0) {
            throw new ValidationException("notification.source-id.positive");
        }
    }

    private static String requireText(String value, String requiredMessage, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(requiredMessage);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ValidationException("notification.title.max.length");
        }
        return normalized;
    }

    private static String normalizeText(String value, int maxLength, String maxLengthMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ValidationException(maxLengthMessage);
        }
        return normalized;
    }
}
