package backend.xxx.chat.ai.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AiRateLimitAspect {

    private final AiRequestLimiter aiRequestLimiter;

    @Around("@annotation(aiRateLimit)")
    public Object checkLimit(ProceedingJoinPoint joinPoint, AiRateLimit aiRateLimit) throws Throwable {
        aiRequestLimiter.check(currentRequest(), aiRateLimit.action());
        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new IllegalStateException("AI rate limiting requires an active HTTP request");
        }
        return attributes.getRequest();
    }
}
