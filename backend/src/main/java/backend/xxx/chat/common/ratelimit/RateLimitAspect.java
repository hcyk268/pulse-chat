package backend.xxx.chat.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@RequiredArgsConstructor
@Aspect
@Component
public class RateLimitAspect {
    
    private final RateLimitProvider rateLimitProvider;
    private final ClientIpResolver clientIpResolver;

    @Around("@annotation(rateLimit)")
    public Object checkLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {

        String action = rateLimit.action();
        int maxReq = rateLimit.maxRequests();
        Duration windowDuration = Duration.ofSeconds(rateLimit.timeWindow());
        String ipClient = clientIpResolver.resolve(currentRequest());

        rateLimitProvider.rateLimit(ipClient, action, maxReq, windowDuration);

        return joinPoint.proceed();
    }

    private jakarta.servlet.http.HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new IllegalStateException("Rate limiting requires an active HTTP request");
        }
        return attributes.getRequest();
    }
}
