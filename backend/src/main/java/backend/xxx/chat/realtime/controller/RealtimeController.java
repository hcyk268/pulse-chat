package backend.xxx.chat.realtime.controller;

import java.security.Principal;

import backend.xxx.chat.common.exception.UnauthorizedException;
import backend.xxx.chat.realtime.dto.TypingStatusRequest;
import backend.xxx.chat.realtime.service.DeliveredService;
import backend.xxx.chat.realtime.service.TypingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@RequiredArgsConstructor
@Validated
public class RealtimeController {

    private final TypingService typingService;
    private final DeliveredService deliveredService;

    @MessageMapping("/conversations/{conversationId}/typing")
    public void updateTyping(
            Principal principal,
            @Positive @DestinationVariable Long conversationId,
            @Valid @Payload TypingStatusRequest request
    ) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        typingService.updateTyping(principal.getName(), conversationId, request);
    }

    @MessageMapping("/messages/{messageId}/delivered")
    public void messageDelivered(
            Principal principal,
            @Positive @DestinationVariable Long messageId
    ) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        deliveredService.messageDelivered(principal.getName(), messageId);
    }
}
