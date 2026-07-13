package backend.xxx.chat.realtime.service;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.realtime.dto.TypingStatusRequest;
import org.springframework.stereotype.Component;

@Component
public class RealtimeValidator {

    public void validateTypingRequest(Long conversationId, TypingStatusRequest request) {
        if (conversationId == null) {
            throw new ValidationException("conversationId must not be null");
        }

        if (request == null || request.typing() == null) {
            throw new ValidationException("typing must not be null");
        }
    }
}
