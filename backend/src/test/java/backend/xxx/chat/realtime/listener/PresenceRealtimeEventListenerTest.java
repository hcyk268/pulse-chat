package backend.xxx.chat.realtime.listener;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.realtime.event.PresenceUpdatedDomainEvent;
import backend.xxx.chat.realtime.model.PresenceUpdatedEventData;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.RealtimeEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceRealtimeEventListenerTest {

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private PresenceRealtimeEventListener listener;

    @Test
    void onPresenceUpdatedPublishesToVisiblePeers() {
        Instant lastActiveAt = Instant.parse("2026-01-01T00:00:00Z");
        PresenceUpdatedDomainEvent event = new PresenceUpdatedDomainEvent(
                1L,
                "alice",
                true,
                lastActiveAt
        );

        when(participantRepository.findVisiblePeerUsernamesByUserId(1L))
                .thenReturn(List.of("bob", "carol"));

        listener.onPresenceUpdated(event);

        ArgumentCaptor<PresenceUpdatedEventData> dataCaptor =
                ArgumentCaptor.forClass(PresenceUpdatedEventData.class);
        verify(realtimeEventPublisher).sendToUsers(
                eq(List.of("bob", "carol")),
                eq(RealtimeEventType.PRESENCE_UPDATED),
                eq(null),
                dataCaptor.capture()
        );

        PresenceUpdatedEventData data = dataCaptor.getValue();
        assertThat(data.userId()).isEqualTo(1L);
        assertThat(data.username()).isEqualTo("alice");
        assertThat(data.isOnline()).isTrue();
        assertThat(data.lastActiveAt()).isEqualTo(lastActiveAt);
    }

    @Test
    void onPresenceUpdatedDoesNothingWhenThereAreNoVisiblePeers() {
        PresenceUpdatedDomainEvent event = new PresenceUpdatedDomainEvent(
                1L,
                "alice",
                false,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        when(participantRepository.findVisiblePeerUsernamesByUserId(1L))
                .thenReturn(List.of());

        listener.onPresenceUpdated(event);

        verifyNoInteractions(realtimeEventPublisher);
    }
}
