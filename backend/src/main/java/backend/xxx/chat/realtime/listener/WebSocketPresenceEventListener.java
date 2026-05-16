package backend.xxx.chat.realtime.listener;

import java.security.Principal;

import backend.xxx.chat.realtime.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        presenceService.resetAllConnections();
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }

        presenceService.markConnected(principal.getName());
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }

        presenceService.markDisconnected(principal.getName());
    }
}
