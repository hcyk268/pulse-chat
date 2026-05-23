package backend.xxx.chat.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Objects;

@RequiredArgsConstructor
@Aspect
@Component
public class RateLimitAspect {
    
    private final RateLimitProvider rateLimitProvider;

    @Around("@annotation(rateLimit)")
    public Object checkLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {

        String action = rateLimit.action();
        int maxReq = rateLimit.maxRequests();
        Duration windowDuration = Duration.ofSeconds(rateLimit.timeWindow());
        String ipClient = this.getIpClient();

        this.rateLimitProvider.rateLimit(ipClient, action, maxReq, windowDuration);

        return joinPoint.proceed();
    }

    private String getIpClient() {
        String[] HEADERS = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        HttpServletRequest request =
                ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                        .getRequest();

        for (String header : HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
