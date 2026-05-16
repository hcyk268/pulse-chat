package backend.xxx.chat.realtime.service;

import java.util.Optional;

import backend.xxx.chat.realtime.event.PresenceUpdatedDomainEvent;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.PresenceRepository;
import backend.xxx.chat.user.service.UserLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private PresenceService presenceService;

    @Test
    void markConnectedCreatesPresenceAndPublishesWhenUserBecomesOnline() {
        User alice = user(1L, "alice");

        when(userLookupService.getCurrentUser("alice")).thenReturn(alice);
        when(presenceRepository.findByUserIdForUpdate(alice.getId())).thenReturn(Optional.empty());

        presenceService.markConnected("alice");

        ArgumentCaptor<Presence> presenceCaptor = ArgumentCaptor.forClass(Presence.class);
        verify(presenceRepository).save(presenceCaptor.capture());

        Presence savedPresence = presenceCaptor.getValue();
        assertThat(savedPresence.getUserId()).isEqualTo(alice.getId());
        assertThat(savedPresence.isOnline()).isTrue();
        assertThat(savedPresence.getConnectionCount()).isEqualTo(1);
        assertThat(savedPresence.getLastActiveAt()).isNotNull();

        ArgumentCaptor<PresenceUpdatedDomainEvent> eventCaptor =
                ArgumentCaptor.forClass(PresenceUpdatedDomainEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        PresenceUpdatedDomainEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(alice.getId());
        assertThat(event.username()).isEqualTo(alice.getUsername());
        assertThat(event.online()).isTrue();
        assertThat(event.lastActiveAt()).isEqualTo(savedPresence.getLastActiveAt());
    }

    @Test
    void markConnectedDoesNotPublishWhenUserAlreadyOnline() {
        User alice = user(1L, "alice");
        Presence presence = Presence.offline(alice);
        presence.markOnline(java.time.Instant.parse("2026-01-01T00:00:00Z"));

        when(userLookupService.getCurrentUser("alice")).thenReturn(alice);
        when(presenceRepository.findByUserIdForUpdate(alice.getId())).thenReturn(Optional.of(presence));

        presenceService.markConnected("alice");

        assertThat(presence.isOnline()).isTrue();
        assertThat(presence.getConnectionCount()).isEqualTo(2);
        verify(presenceRepository).save(presence);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void markDisconnectedDoesNotPublishUntilLastConnectionCloses() {
        User alice = user(1L, "alice");
        Presence presence = Presence.offline(alice);
        presence.markOnline(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        presence.markOnline(java.time.Instant.parse("2026-01-01T00:01:00Z"));

        when(userLookupService.getCurrentUser("alice")).thenReturn(alice);
        when(presenceRepository.findByUserIdForUpdate(alice.getId())).thenReturn(Optional.of(presence));

        presenceService.markDisconnected("alice");

        assertThat(presence.isOnline()).isTrue();
        assertThat(presence.getConnectionCount()).isEqualTo(1);
        verify(presenceRepository).save(presence);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void markDisconnectedPublishesWhenLastConnectionCloses() {
        User alice = user(1L, "alice");
        Presence presence = Presence.offline(alice);
        presence.markOnline(java.time.Instant.parse("2026-01-01T00:00:00Z"));

        when(userLookupService.getCurrentUser("alice")).thenReturn(alice);
        when(presenceRepository.findByUserIdForUpdate(alice.getId())).thenReturn(Optional.of(presence));

        presenceService.markDisconnected("alice");

        assertThat(presence.isOnline()).isFalse();
        assertThat(presence.getConnectionCount()).isZero();

        ArgumentCaptor<PresenceUpdatedDomainEvent> eventCaptor =
                ArgumentCaptor.forClass(PresenceUpdatedDomainEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        PresenceUpdatedDomainEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(alice.getId());
        assertThat(event.username()).isEqualTo(alice.getUsername());
        assertThat(event.online()).isFalse();
        assertThat(event.lastActiveAt()).isEqualTo(presence.getLastActiveAt());
    }

    @Test
    void resetAllConnectionsDelegatesToRepository() {
        presenceService.resetAllConnections();

        verify(presenceRepository).resetAllConnections();
        verifyNoInteractions(userLookupService, applicationEventPublisher);
    }

    private User user(Long id, String username) {
        User user = User.create(
                username,
                username + "@example.com",
                "hashed-password",
                username
        );
        user.setId(id);
        return user;
    }
}
