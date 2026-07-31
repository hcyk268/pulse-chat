package backend.xxx.chat.notification.dto;

import java.time.Instant;

public record NotificationReadAllResponse(
        int affectedCount,
        long unreadCount,
        Instant readAt
) {
}
