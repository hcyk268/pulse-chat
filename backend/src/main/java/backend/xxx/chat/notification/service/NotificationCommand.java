package backend.xxx.chat.notification.service;

import backend.xxx.chat.notification.model.NotificationTargetType;
import backend.xxx.chat.notification.model.NotificationType;
import backend.xxx.chat.user.model.User;

public record NotificationCommand(
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
}
