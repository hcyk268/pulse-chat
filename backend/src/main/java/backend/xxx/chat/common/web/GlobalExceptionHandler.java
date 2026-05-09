package backend.xxx.chat.common.web;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.common.dto.ApiErrorResponse;
import backend.xxx.chat.common.dto.ApiFieldErrorResponse;
import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(buildErrorResponse(
                        ex.getCode(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        ex.getFieldErrors().isEmpty() ? null : ex.getFieldErrors()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ApiFieldErrorResponse> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorResponse)
                .toList();

        return ResponseEntity.badRequest()
                .body(buildErrorResponse(
                        ErrorCode.VALIDATION_ERROR,
                        "Request validation failed",
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ApiFieldErrorResponse> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ApiFieldErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return ResponseEntity.badRequest()
                .body(buildErrorResponse(
                        ErrorCode.VALIDATION_ERROR,
                        "Request validation failed",
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(buildErrorResponse(
                        ErrorCode.VALIDATION_ERROR,
                        ex.getMessage(),
                        request.getRequestURI(),
                        null
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(
                        ErrorCode.UNAUTHORIZED,
                        "Invalid username/email or password",
                        request.getRequestURI(),
                        null
                ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabledException(
            DisabledException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse(
                        ErrorCode.ACCOUNT_INACTIVE,
                        "Account is inactive",
                        request.getRequestURI(),
                        null
                ));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLockedException(
            LockedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse(
                        ErrorCode.ACCOUNT_LOCKED,
                        "Account is locked",
                        request.getRequestURI(),
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Unexpected server error",
                        request.getRequestURI(),
                        null
                ));
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
}
