package backend.xxx.chat.notification.service;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.notification.dto.NotificationDeletedEventData;
import backend.xxx.chat.notification.dto.NotificationResponse;
import backend.xxx.chat.notification.model.Notification;
import backend.xxx.chat.notification.model.NotificationTargetType;
import backend.xxx.chat.notification.model.NotificationType;
import backend.xxx.chat.notification.repository.NotificationRepository;
import backend.xxx.chat.user.model.User;
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

    @Test
    @SuppressWarnings("unchecked")
    void createsNotificationBatchWithOneDedupeLookup() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        NotificationCommand aliceCommand = command(alice, "mention:1:1");
        NotificationCommand bobCommand = command(bob, "mention:1:2");
        NotificationRepository.NotificationDedupeKey existing =
                mock(NotificationRepository.NotificationDedupeKey.class);
        NotificationResponse bobResponse = mock(NotificationResponse.class);

        when(existing.getRecipientId()).thenReturn(1L);
        when(existing.getDedupeKey()).thenReturn("mention:1:1");
        when(notificationRepository.findExistingDedupeKeys(
                java.util.Set.of(1L, 2L),
                java.util.Set.of("mention:1:1", "mention:1:2")
        )).thenReturn(List.of(existing));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Notification> values = invocation.getArgument(0);
            java.util.ArrayList<Notification> saved = new java.util.ArrayList<>();
            values.forEach(saved::add);
            return saved;
        });
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(bobResponse);

        List<NotificationResponse> responses = service.createAll(List.of(aliceCommand, bobCommand));

        assertThat(responses).containsExactly(bobResponse);
        ArgumentCaptor<Iterable<Notification>> batchCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).hasSize(1);
        verify(realtimeNotifier).created("bob", bobResponse);
    }

    private NotificationCommand command(User recipient, String dedupeKey) {
        return new NotificationCommand(
                recipient,
                null,
                NotificationType.MENTION,
                "Mention",
                "You were mentioned.",
                NotificationTargetType.MESSAGE,
                1L,
                "MESSAGE",
                1L,
                dedupeKey
        );
    }

    private User user(Long id, String username) {
        User user = User.create(
                username,
                username + "@example.com",
                "Password123!",
                username
        );
        user.setId(id);
        return user;
    }

    private Notification notification(Long id) {
        Notification notification = mock(Notification.class);
        when(notification.getId()).thenReturn(id);
        return notification;
    }
}
