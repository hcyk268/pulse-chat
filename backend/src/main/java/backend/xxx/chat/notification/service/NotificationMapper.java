package backend.xxx.chat.notification.service;

import backend.xxx.chat.notification.dto.NotificationActorResponse;
import backend.xxx.chat.notification.dto.NotificationResponse;
import backend.xxx.chat.notification.model.Notification;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                toActorResponse(notification.getActor()),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getSourceType(),
                notification.getSourceId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }

    private NotificationActorResponse toActorResponse(User actor) {
        if (actor == null) {
            return null;
        }
        return new NotificationActorResponse(
                actor.getId(),
                actor.getUsername(),
                actor.getDisplayName(),
                actor.getAvatarUrl()
        );
    }
}
