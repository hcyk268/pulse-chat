package backend.xxx.chat.notification.dto;

public record NotificationDeletedEventData(
        Long notificationId,
        long unreadCount
) {
}
