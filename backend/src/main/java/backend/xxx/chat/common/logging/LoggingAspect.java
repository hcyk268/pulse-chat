package backend.xxx.chat.common.logging;

import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private static final Set<String> SENSITIVE_NAMES = Set.of(
            "password",
            "token",
            "secret",
            "credential",
            "authorization",
            "content"
    );

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerBeans() {
    }

    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceBeans() {
    }

    @Around("controllerBeans() || serviceBeans()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = AopUtils.getTargetClass(joinPoint.getTarget());
        boolean controller = AnnotationUtils.findAnnotation(targetClass, RestController.class) != null;
        boolean service = AnnotationUtils.findAnnotation(targetClass, Service.class) != null;
        String layer = controller ? "controller" : "service";
        String invocation = targetClass.getSimpleName() + "." + signature.getName();
        String request = currentRequest();
        String requestLabel = requestLabel(request);
        String args = summarizeArguments(joinPoint.getArgs());

        if (controller) {
            log.info("Started {} {} request={} args={}", layer, invocation, requestLabel, args);
        } else if (service && hasHttpRequest(request) && log.isDebugEnabled()) {
            log.debug("Started {} {} args={}", layer, invocation, args);
        }

        long startedAt = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = elapsedMs(startedAt);

            if (controller) {
                log.info(
                        "Completed {} {} request={} status={} elapsedMs={}",
                        layer,
                        invocation,
                        requestLabel,
                        resolveHttpStatus(result),
                        elapsedMs
                );
            } else if (service && hasHttpRequest(request) && log.isDebugEnabled()) {
                log.debug("Completed {} {} elapsedMs={}", layer, invocation, elapsedMs);
            }

            return result;
        } catch (Throwable ex) {
            long elapsedMs = elapsedMs(startedAt);
            log.warn(
                    "Failed {} {} request={} exception={} message={} elapsedMs={}",
                    layer,
                    invocation,
                    requestLabel,
                    ex.getClass().getSimpleName(),
                    summarizeExceptionMessage(ex),
                    elapsedMs
            );
            throw ex;
        }
    }

    private static long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private static boolean hasHttpRequest(String request) {
        return request != null;
    }

    private static String requestLabel(String request) {
        return request == null ? "none" : request;
    }

    private static String currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String method = request.getMethod();
            String requestUri = request.getRequestURI();

            if (method == null || method.isBlank() || requestUri == null || requestUri.isBlank()) {
                return null;
            }

            return method + " " + requestUri;
        }

        return null;
    }

    private static Integer resolveHttpStatus(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }

        return null;
    }

    private static String summarizeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        return Arrays.stream(args)
                .map(LoggingAspect::summarizeValue)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String summarizeValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof HttpServletRequest) {
            return "HttpServletRequest";
        }

        if (value instanceof HttpServletResponse) {
            return "HttpServletResponse";
        }

        if (value instanceof CharSequence text) {
            return summarizeText(text);
        }

        if (value instanceof Number || value instanceof Boolean || value instanceof UUID || value instanceof Enum<?>) {
            return value.toString();
        }

        if (value instanceof TemporalAccessor) {
            return value.toString();
        }

        if (value instanceof Collection<?> collection) {
            return value.getClass().getSimpleName() + "(size=" + collection.size() + ")";
        }

        if (value instanceof Map<?, ?> map) {
            return value.getClass().getSimpleName() + "(size=" + map.size() + ")";
        }

        Class<?> valueType = value.getClass();
        if (valueType.isRecord()) {
            return summarizeRecord(value, valueType);
        }

        return valueType.getSimpleName();
    }

    private static String summarizeRecord(Object record, Class<?> recordType) {
        String components = Arrays.stream(recordType.getRecordComponents())
                .map(component -> summarizeRecordComponent(record, component))
                .collect(Collectors.joining(", "));

        return recordType.getSimpleName() + "(" + components + ")";
    }

    private static String summarizeRecordComponent(Object record, RecordComponent component) {
        String name = component.getName();
        if (isSensitiveName(name)) {
            return name + "=<redacted>";
        }

        try {
            return name + "=" + summarizeValue(component.getAccessor().invoke(record));
        } catch (ReflectiveOperationException ex) {
            return name + "=<unavailable>";
        }
    }

    private static boolean isSensitiveName(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return SENSITIVE_NAMES.stream().anyMatch(normalizedName::contains);
    }

    private static String summarizeText(CharSequence text) {
        return "String(length=" + text.length() + ")";
    }

    private static String summarizeExceptionMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "<empty>";
        }

        return summarizeText(message);
    }
}
