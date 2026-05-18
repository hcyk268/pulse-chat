package backend.xxx.chat.realtime.controller;

import backend.xxx.chat.common.dto.ApiErrorResponse;
import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeExceptionHandlerTest {

    private final RealtimeExceptionHandler handler = new RealtimeExceptionHandler();

    @Test
    void handleApiExceptionReturnsApiErrorResponseForUserErrorQueue() throws NoSuchMethodException {
        Message<byte[]> message = message("/app/messages/100/delivered");
        ApiException exception = new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "Forbidden"
        );

        ApiErrorResponse response = handler.handleApiException(exception, message);

        assertThat(response.code()).isEqualTo(ErrorCode.FORBIDDEN.name());
        assertThat(response.message()).isEqualTo("Forbidden");
        assertThat(response.path()).isEqualTo("/app/messages/100/delivered");
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.fieldErrors()).isNull();

        SendToUser sendToUser = RealtimeExceptionHandler.class
                .getMethod("handleApiException", ApiException.class, Message.class)
                .getAnnotation(SendToUser.class);
        assertThat(sendToUser.destinations()).containsExactly("/queue/errors");
        assertThat(sendToUser.broadcast()).isFalse();
    }

    @Test
    void handleUnexpectedExceptionHidesInternalExceptionMessage() {
        Message<byte[]> message = message("/app/conversations/10/typing");

        ApiErrorResponse response = handler.handleUnexpectedException(
                new RuntimeException("database details"),
                message
        );

        assertThat(response.code()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.name());
        assertThat(response.message()).isEqualTo("Unexpected server error");
        assertThat(response.path()).isEqualTo("/app/conversations/10/typing");
    }

    private Message<byte[]> message(String destination) {
        return MessageBuilder.withPayload(new byte[0])
                .setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, destination)
                .build();
    }
}
