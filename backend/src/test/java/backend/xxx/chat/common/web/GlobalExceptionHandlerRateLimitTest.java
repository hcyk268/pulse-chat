package backend.xxx.chat.common.web;

import backend.xxx.chat.common.dto.ApiErrorResponse;
import backend.xxx.chat.common.exception.LimitExceedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerRateLimitTest {

    @Test
    void addsRateLimitHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        ResponseEntity<ApiErrorResponse> response =
                new GlobalExceptionHandler().handleLimitExceeded(new LimitExceedException(42), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("42");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
    }
}
