package backend.xxx.chat.common.ratelimit;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    static final String UNKNOWN_IP = "unknown";

    public String resolve(HttpServletRequest request) {
        if (request == null || request.getRemoteAddr() == null || request.getRemoteAddr().isBlank()) {
            return UNKNOWN_IP;
        }

        String address = request.getRemoteAddr().trim().toLowerCase(Locale.ROOT);
        int zoneIndex = address.indexOf('%');
        if (zoneIndex >= 0) {
            address = address.substring(0, zoneIndex);
        }

        return address.startsWith("::ffff:") ? address.substring(7) : address;
    }
}
