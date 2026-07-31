package backend.xxx.chat.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> items,
        Long nextBeforeId,
        boolean hasMore,
        long unreadCount
) {
}
