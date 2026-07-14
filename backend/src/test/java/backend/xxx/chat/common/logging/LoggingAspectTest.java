package backend.xxx.chat.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private final LoggingAspect loggingAspect = new LoggingAspect();
    private Logger logger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() throws Throwable {
        logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);

        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(methodSignature.getName()).thenReturn("doWork");
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn("ok");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        logger.detachAppender(listAppender);
        logger.setLevel(originalLevel);
    }

    @Test
    void skipsDebugTracingForBackgroundServiceInvocation() throws Throwable {
        loggingAspect.logAround(joinPoint);

        assertThat(loggedMessages()).isEmpty();
    }

    @Test
    void keepsDebugTracingForServiceInvocationInsideHttpRequest() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/messages");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        loggingAspect.logAround(joinPoint);

        assertThat(loggedMessages())
                .anyMatch(message -> message.contains("Started service TestService.doWork"))
                .anyMatch(message -> message.contains("Completed service TestService.doWork"));
    }

    private List<String> loggedMessages() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    @Service
    private static class TestService {

        @SuppressWarnings("unused")
        String doWork() {
            return "ok";
        }
    }
}
