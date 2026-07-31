package backend.xxx.chat.notification.service;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.notification.dto.NotificationDeletedEventData;
import backend.xxx.chat.notification.dto.NotificationResponse;
import backend.xxx.chat.notification.model.Notification;
import backend.xxx.chat.notification.repository.NotificationRepository;
import backend.xxx.chat.user.service.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationMapper notificationMapper;
    private NotificationRealtimeNotifier realtimeNotifier;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationMapper = mock(NotificationMapper.class);
        realtimeNotifier = mock(NotificationRealtimeNotifier.class);
        service = new NotificationService(
                notificationRepository,
                mock(UserLookupService.class),
                notificationMapper,
                realtimeNotifier
        );
    }

    @Test
    void returnsNotificationPage() {
        Notification first = notification(5L);
        Notification second = notification(4L);
        Notification extra = notification(3L);
        NotificationResponse firstResponse = mock(NotificationResponse.class);
        NotificationResponse secondResponse = mock(NotificationResponse.class);

        when(notificationRepository.findPageByRecipientUsername(
                eq("alice"),
                eq(10L),
                any(Pageable.class)
        )).thenReturn(List.of(first, second, extra));
        when(notificationMapper.toResponse(first)).thenReturn(firstResponse);
        when(notificationMapper.toResponse(second)).thenReturn(secondResponse);
        when(notificationRepository.countByRecipient_UsernameIgnoreCaseAndReadAtIsNull("alice"))
                .thenReturn(7L);

        var response = service.getNotifications("alice", (short) 2, 10L);

        assertThat(response.items()).containsExactly(firstResponse, secondResponse);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextBeforeId()).isEqualTo(4L);
        assertThat(response.unreadCount()).isEqualTo(7L);
    }

    @Test
    void deletesNotification() {
        Notification notification = notification(9L);
        when(notificationRepository.findByRecipient_UsernameIgnoreCaseAndId("alice", 9L))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.countByRecipient_UsernameIgnoreCaseAndReadAtIsNull("alice"))
                .thenReturn(3L);

        service.deleteNotification("alice", 9L);

        verify(notificationRepository).delete(notification);
        ArgumentCaptor<NotificationDeletedEventData> captor =
                ArgumentCaptor.forClass(NotificationDeletedEventData.class);
        verify(realtimeNotifier).deleted(eq("alice"), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new NotificationDeletedEventData(9L, 3L));
    }

    private Notification notification(Long id) {
        Notification notification = mock(Notification.class);
        when(notification.getId()).thenReturn(id);
        return notification;
    }
}
