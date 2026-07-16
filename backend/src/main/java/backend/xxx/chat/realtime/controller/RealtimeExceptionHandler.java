package backend.xxx.chat.realtime.controller;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.common.dto.ApiErrorResponse;
import backend.xxx.chat.common.dto.ApiFieldErrorResponse;
import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class RealtimeExceptionHandler {

    static final String USER_ERRORS_DESTINATION = "/queue/errors";

    @MessageExceptionHandler(ApiException.class)
    @SendToUser(destinations = USER_ERRORS_DESTINATION, broadcast = false)
    public ApiErrorResponse handleApiException(ApiException exception, Message<?> message) {
        return buildErrorResponse(
                exception.getCode(),
                exception.getMessage(),
                getDestination(message),
                exception.getFieldErrors().isEmpty() ? null : exception.getFieldErrors()
        );
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = USER_ERRORS_DESTINATION, broadcast = false)
    public ApiErrorResponse handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            Message<?> message
    ) {
        List<ApiFieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorResponse)
                .toList();

        return buildErrorResponse(
                ErrorCode.VALIDATION_ERROR,
                "error.validation.request.failed",
                getDestination(message),
                fieldErrors
        );
    }

    @MessageExceptionHandler(MethodArgumentTypeMismatchException.class)
    @SendToUser(destinations = USER_ERRORS_DESTINATION, broadcast = false)
    public ApiErrorResponse handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            Message<?> message
    ) {
        return buildErrorResponse(
                ErrorCode.VALIDATION_ERROR,
                exception.getMessage(),
                getDestination(message),
                null
        );
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser(destinations = USER_ERRORS_DESTINATION, broadcast = false)
    public ApiErrorResponse handleIllegalArgumentException(
            IllegalArgumentException exception,
            Message<?> message
    ) {
        return buildErrorResponse(
                ErrorCode.VALIDATION_ERROR,
                exception.getMessage(),
                getDestination(message),
                null
        );
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = USER_ERRORS_DESTINATION, broadcast = false)
    public ApiErrorResponse handleUnexpectedException(Exception exception, Message<?> message) {
        return buildErrorResponse(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "error.server.unexpected",
                getDestination(message),
                null
        );
    }

    private ApiFieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new ApiFieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ApiErrorResponse buildErrorResponse(
            ErrorCode code,
            String message,
            String path,
            List<ApiFieldErrorResponse> fieldErrors
    ) {
        return new ApiErrorResponse(code.name(), message, Instant.now(), path, fieldErrors);
    }

    private String getDestination(Message<?> message) {
        StompHeaderAccessor stompAccessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );
        if (stompAccessor != null && stompAccessor.getDestination() != null) {
            return stompAccessor.getDestination();
        }

        Object destination = message.getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER);
        return destination instanceof String value ? value : null;
    }
}
