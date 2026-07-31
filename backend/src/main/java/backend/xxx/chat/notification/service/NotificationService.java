package backend.xxx.chat.notification.service;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.notification.dto.NotificationDeletedEventData;
import backend.xxx.chat.notification.dto.NotificationListResponse;
import backend.xxx.chat.notification.dto.NotificationReadAllResponse;
import backend.xxx.chat.notification.dto.NotificationResponse;
import backend.xxx.chat.notification.dto.NotificationUnreadCountResponse;
import backend.xxx.chat.notification.model.Notification;
import backend.xxx.chat.notification.repository.NotificationRepository;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final UserLookupService userLookupService;
    private final NotificationMapper notificationMapper;
    private final NotificationRealtimeNotifier realtimeNotifier;

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(String username, Short limit, Long beforeId) {
        int pageLimit = normalizeLimit(limit);
        validateBeforeId(beforeId);
        List<Notification> notifications = notificationRepository.findPageByRecipientUsername(
                username,
                beforeId,
                PageRequest.of(0, pageLimit + 1)
        );
        boolean hasMore = notifications.size() > pageLimit;
        List<Notification> page = hasMore ? notifications.subList(0, pageLimit) : notifications;
        List<NotificationResponse> items = page.stream()
                .map(notificationMapper::toResponse)
                .toList();
        Long nextBeforeId = hasMore && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        return new NotificationListResponse(items, nextBeforeId, hasMore, unreadCount(username));
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(String username, Long notificationId) {
        return notificationMapper.toResponse(findOwnedNotification(username, notificationId));
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(String username) {
        return new NotificationUnreadCountResponse(unreadCount(username));
    }

    @Transactional
    public NotificationResponse markRead(String username, Long notificationId) {
        Notification notification = findOwnedNotification(username, notificationId);
        if (notification.markRead(Instant.now())) {
            NotificationResponse response = notificationMapper.toResponse(notification);
            realtimeNotifier.updated(username, response);
            return response;
        }
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public NotificationResponse markUnread(String username, Long notificationId) {
        Notification notification = findOwnedNotification(username, notificationId);
        if (notification.markUnread()) {
            NotificationResponse response = notificationMapper.toResponse(notification);
            realtimeNotifier.updated(username, response);
            return response;
        }
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public NotificationReadAllResponse markAllRead(String username) {
        User recipient = userLookupService.getCurrentUser(username);
        Instant readAt = Instant.now();
        int affectedCount = notificationRepository.markAllReadByRecipientId(recipient.getId(), readAt);
        NotificationReadAllResponse response = new NotificationReadAllResponse(affectedCount, 0L, readAt);
        if (affectedCount > 0) {
            realtimeNotifier.readAll(username, response);
        }
        return response;
    }

    @Transactional
    public void deleteNotification(String username, Long notificationId) {
        Notification notification = findOwnedNotification(username, notificationId);
        notificationRepository.delete(notification);
        long nextUnreadCount = unreadCount(username);
        realtimeNotifier.deleted(
                username,
                new NotificationDeletedEventData(notificationId, Math.max(0, nextUnreadCount))
        );
    }

    @Transactional
    public NotificationResponse create(NotificationCommand command) {
        if (command.dedupeKey() != null
                && notificationRepository.existsByRecipient_IdAndDedupeKey(
                        command.recipient().getId(),
                        command.dedupeKey()
                )) {
            return null;
        }

        Notification notification = Notification.create(
                command.recipient(),
                command.actor(),
                command.type(),
                command.title(),
                command.body(),
                command.targetType(),
                command.targetId(),
                command.sourceType(),
                command.sourceId(),
                command.dedupeKey()
        );
        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toResponse(saved);
        realtimeNotifier.created(command.recipient().getUsername(), response);
        return response;
    }

    private Notification findOwnedNotification(String username, Long notificationId) {
        if (notificationId == null || notificationId <= 0) {
            throw new ValidationException("notification.id.positive");
        }
        return notificationRepository.findByRecipient_UsernameIgnoreCaseAndId(username, notificationId)
                .orElseThrow(() -> new NotFoundException("notification.not.found"));
    }

    private long unreadCount(String username) {
        return notificationRepository.countByRecipient_UsernameIgnoreCaseAndReadAtIsNull(username);
    }

    private void validateBeforeId(Long beforeId) {
        if (beforeId != null && beforeId <= 0) {
            throw new ValidationException("notification.before-id.positive");
        }
    }

    private int normalizeLimit(Short limit) {
        int value = limit == null ? DEFAULT_LIMIT : limit;
        if (value < 1 || value > MAX_LIMIT) {
            throw new ValidationException("notification.limit.range");
        }
        return value;
    }
}
