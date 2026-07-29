package backend.xxx.chat.auth.service;

import backend.xxx.chat.auth.model.RequestMetadata;
import backend.xxx.chat.common.ratelimit.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class RequestMetadataResolver {

    private final ClientIpResolver clientIpResolver;

    public RequestMetadata current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new RequestMetadata("unknown", "unknown");
        }

        HttpServletRequest request = attributes.getRequest();
        return new RequestMetadata(
                clientIpResolver.resolve(request),
                safeValue(request.getHeader("User-Agent"))
        );
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
