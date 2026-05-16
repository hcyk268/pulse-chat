package backend.xxx.chat.realtime.service;

import java.time.Instant;

import backend.xxx.chat.realtime.event.PresenceUpdatedDomainEvent;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.PresenceRepository;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final UserLookupService userLookupService;
    private final PresenceRepository presenceRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void markConnected(String username) {
        updatePresence(username, true);
    }

    @Transactional
    public void markDisconnected(String username) {
        updatePresence(username, false);
    }

    @Transactional
    public void resetAllConnections() {
        presenceRepository.resetAllConnections();
    }

    private void updatePresence(String username, boolean connected) {
        User user = userLookupService.getCurrentUser(username);
        Presence presence = presenceRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> Presence.offline(user));

        boolean wasOnline = presence.isOnline();
        Instant lastActiveAt = Instant.now();

        if (connected) {
            presence.markOnline(lastActiveAt);
        } else {
            presence.markOffline(lastActiveAt);
        }

        presenceRepository.save(presence);

        if (wasOnline != presence.isOnline()) {
            applicationEventPublisher.publishEvent(
                    new PresenceUpdatedDomainEvent(
                            user.getId(),
                            user.getUsername(),
                            presence.isOnline(),
                            presence.getLastActiveAt()
                    )
            );
        }
    }
}
