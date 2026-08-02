package backend.xxx.chat.realtime.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Realtime", description = "WebSocket realtime message APIs")
@Controller
@RequiredArgsConstructor
@Validated
public class RealtimeController {

    private final TypingService typingService;
    private final DeliveredService deliveredService;

    @Operation(summary = "Update typing status")
    @MessageMapping("/conversations/{conversationId}/typing")
    public void updateTyping(
            Principal principal,
            @Positive @DestinationVariable Long conversationId,
            @Valid @Payload TypingStatusRequest request
    ) {
        if (principal == null) {
            throw new UnauthorizedException("auth.unauthorized");
        }

        typingService.updateTyping(principal.getName(), conversationId, request);
    }

    @Operation(summary = "Mark message delivered")
    @MessageMapping("/messages/{messageId}/delivered")
    public void messageDelivered(
            Principal principal,
            @Positive @DestinationVariable Long messageId
    ) {
        if (principal == null) {
            throw new UnauthorizedException("auth.unauthorized");
        }

        deliveredService.messageDelivered(principal.getName(), messageId);
    }
}
