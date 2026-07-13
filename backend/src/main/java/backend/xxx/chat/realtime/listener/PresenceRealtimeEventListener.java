package backend.xxx.chat.realtime.listener;

import java.util.List;

import backend.xxx.chat.conversation.model.ParticipantStatus;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.realtime.event.PresenceUpdatedDomainEvent;
import backend.xxx.chat.realtime.model.PresenceUpdatedEventData;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PresenceRealtimeEventListener {

    private final ConversationParticipantRepository participantRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onPresenceUpdated(PresenceUpdatedDomainEvent event) {
        List<String> peerUsernames = participantRepository.findVisiblePeerUsernamesByUserId(event.userId(), ParticipantStatus.ACTIVE);
        if (peerUsernames.isEmpty()) {
            return;
        }

        PresenceUpdatedEventData data = new PresenceUpdatedEventData(
                event.userId(),
                event.username(),
                event.online(),
                event.lastActiveAt()
        );

        realtimeEventPublisher.sendToUsers(
                peerUsernames,
                RealtimeEventType.PRESENCE_UPDATED,
                null,
                data
        );
    }
}
