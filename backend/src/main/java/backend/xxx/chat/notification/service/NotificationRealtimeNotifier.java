package backend.xxx.chat.notification.service;

import backend.xxx.chat.notification.dto.NotificationDeletedEventData;
import backend.xxx.chat.notification.dto.NotificationReadAllResponse;
import backend.xxx.chat.notification.dto.NotificationResponse;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRealtimeNotifier {

    private final RealtimeEventPublisher realtimeEventPublisher;

    public void created(String username, NotificationResponse response) {
        afterCommit(() -> send(username, response.id(), RealtimeEventType.NOTIFICATION_CREATED, response));
    }

    public void updated(String username, NotificationResponse response) {
        afterCommit(() -> send(username, response.id(), RealtimeEventType.NOTIFICATION_UPDATED, response));
    }

    public void readAll(String username, NotificationReadAllResponse response) {
        afterCommit(() -> realtimeEventPublisher.sendToUser(
                "notification_read_all_" + response.readAt().toEpochMilli(),
                username,
                RealtimeEventType.NOTIFICATION_READ_ALL,
                null,
                response
        ));
    }

    public void deleted(String username, NotificationDeletedEventData data) {
        afterCommit(() -> send(username, data.notificationId(), RealtimeEventType.NOTIFICATION_DELETED, data));
    }

    private <T> void send(String username, Long notificationId, RealtimeEventType eventType, T data) {
        realtimeEventPublisher.sendToUser(
                "notification_" + notificationId + "_" + eventType.name().toLowerCase(),
                username,
                eventType,
                null,
                data
        );
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            runSafely(action);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runSafely(action);
            }
        });
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.warn("Could not publish notification realtime event", exception);
        }
    }
}
