package backend.xxx.chat.notification.dto;

import java.time.Instant;

import backend.xxx.chat.notification.model.NotificationTargetType;
import backend.xxx.chat.notification.model.NotificationType;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        NotificationActorResponse actor,
        NotificationTargetType targetType,
        Long targetId,
        String sourceType,
        Long sourceId,
        boolean read,
        Instant readAt,
        Instant createdAt,
        Instant updatedAt
) {
}
