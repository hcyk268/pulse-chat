package backend.xxx.chat.notification.controller;

import backend.xxx.chat.common.dto.ResponseData;
import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.notification.dto.NotificationListResponse;
import backend.xxx.chat.notification.dto.NotificationReadAllResponse;
import backend.xxx.chat.notification.dto.NotificationResponse;
import backend.xxx.chat.notification.dto.NotificationUnreadCountResponse;
import backend.xxx.chat.notification.service.NotificationService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;

    @GetMapping
    public ResponseData<NotificationListResponse> getNotifications(
            @RequestParam(required = false) Short limit,
            @RequestParam(required = false) Long beforeId
    ) {
        return new ResponseData<>(
                true,
                "notification.list.success",
                notificationService.getNotifications(currentUserProvider.getCurrentUsername(), limit, beforeId)
        );
    }

    @GetMapping("/unread-count")
    public ResponseData<NotificationUnreadCountResponse> getUnreadCount() {
        return new ResponseData<>(
                true,
                "notification.unread-count.success",
                notificationService.getUnreadCount(currentUserProvider.getCurrentUsername())
        );
    }

    @GetMapping("/{id}")
    public ResponseData<NotificationResponse> getNotification(@Positive @PathVariable Long id) {
        return new ResponseData<>(
                true,
                "notification.detail.success",
                notificationService.getNotification(currentUserProvider.getCurrentUsername(), id)
        );
    }

    @PatchMapping("/{id}/read")
    public ResponseData<NotificationResponse> markRead(@Positive @PathVariable Long id) {
        return new ResponseData<>(
                true,
                "notification.read.success",
                notificationService.markRead(currentUserProvider.getCurrentUsername(), id)
        );
    }

    @PatchMapping("/{id}/unread")
    public ResponseData<NotificationResponse> markUnread(@Positive @PathVariable Long id) {
        return new ResponseData<>(
                true,
                "notification.unread.success",
                notificationService.markUnread(currentUserProvider.getCurrentUsername(), id)
        );
    }

    @PatchMapping("/read-all")
    public ResponseData<NotificationReadAllResponse> markAllRead() {
        return new ResponseData<>(
                true,
                "notification.read-all.success",
                notificationService.markAllRead(currentUserProvider.getCurrentUsername())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseData<Void> deleteNotification(@Positive @PathVariable Long id) {
        notificationService.deleteNotification(currentUserProvider.getCurrentUsername(), id);
        return new ResponseData<>(true, "notification.delete.success");
    }
}
