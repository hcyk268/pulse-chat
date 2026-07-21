package backend.xxx.chat.market.stream;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class BinanceStreamSessionManager {

    private final AtomicReference<WebSocketSession> sessionReference = new AtomicReference<>();

    public void setSession(WebSocketSession session) {
        sessionReference.set(session);
    }

    public void clearSession(WebSocketSession session) {
        sessionReference.compareAndSet(session, null);
    }

    public boolean isConnected() {
        WebSocketSession session = sessionReference.get();
        return session != null && session.isOpen();
    }

    public void close() throws IOException {
        WebSocketSession session = sessionReference.getAndSet(null);
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}