package backend.xxx.chat.message.controller;

import backend.xxx.chat.common.dto.ResponseData;
import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.message.dto.*;
import backend.xxx.chat.message.model.MessageReactionEmoji;
import backend.xxx.chat.message.service.MessageReactionService;
import backend.xxx.chat.message.service.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Validated
public class MessageController {

    private final CurrentUserProvider currentUserProvider;
    private final MessageService messageService;
    private final MessageReactionService messageReactionService;

    @GetMapping()
    public ResponseData<MessageHistoryResponse> getHistory(
            @Positive @RequestParam(name = "conversationId") Long conversationId,
            @Min(1) @Max(50) @RequestParam(name = "limit", required = false, defaultValue = "20") Short limit,
            @RequestParam(name = "cursor", required = false) String cursor
    ) {
        return new ResponseData<>(true, "Get message history successfully", messageService.getHistory(currentUserProvider.getCurrentUsername(), conversationId, limit, cursor));
    }

    @PostMapping()
    public ResponseEntity<ResponseData<MessageResponse>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseData<>(true, "Send message successfully", messageService.sendMessage(currentUserProvider.getCurrentUsername(), request)));
    }

    @PostMapping("/read")
    public ResponseData<MarkReadResponse> readMessage(@Valid @RequestBody MarkReadRequest request) {
        return new ResponseData<>(true, "Read message successfully", messageService.readMessage(currentUserProvider.getCurrentUsername(), request));
    }

    @GetMapping("/{messageId}/reads")
    public ResponseData<MessageReadReceiptsResponse> getReadReceipts(@Positive @PathVariable Long messageId) {
        return new ResponseData<>(true, "Get message read receipts successfully", messageService.getReadReceipts(
                currentUserProvider.getCurrentUsername(),
                messageId
        ));
    }

    @PostMapping("/{messageId}/pin")
    public ResponseEntity<ResponseData<MessagePinResponse>> pinMessage(@Positive @PathVariable Long messageId) {
        MessageService.PinMessageResult result =
                messageService.pinMessage(currentUserProvider.getCurrentUsername(), messageId);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created() ? "Pin message successfully" : "Message already pinned";
        return ResponseEntity.status(status).body(new ResponseData<>(true, message, result.response()));
    }

    @DeleteMapping("/{messageId}/pin")
    public ResponseData<UnPinMessageResponse> unPinMessage(@Positive @PathVariable Long messageId) {
        return new ResponseData<>(true, "Unpin message successfully", messageService.unPinMessage(currentUserProvider.getCurrentUsername(), messageId));
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<ResponseData<MessageReactionResponse>> reactMessage(
            @Positive @PathVariable Long messageId,
            @Valid @RequestBody MessageReactionRequest request
    ) {
        MessageReactionService.ReactMessageResult result = messageReactionService.reactMessage(
                currentUserProvider.getCurrentUsername(),
                messageId,
                request
        );

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created() ? "React message successfully" : "Update message reaction successfully";
        return ResponseEntity.status(status).body(new ResponseData<>(true, message, result.response()));
    }

    @DeleteMapping("/{messageId}/reactions/{emoji}")
    public ResponseData<Void> removeReaction(
            @Positive @PathVariable Long messageId,
            @PathVariable MessageReactionEmoji emoji
    ) {
        messageReactionService.removeReaction(
                currentUserProvider.getCurrentUsername(),
                messageId,
                emoji
        );
        return new ResponseData<>(true, "Remove message reaction successfully");
    }

    @GetMapping("/{messageId}/reactions")
    public ResponseData<MessageReactionsResponse> getReactions(@Positive @PathVariable Long messageId) {
        return new ResponseData<>(true, "Get message reactions successfully", messageReactionService.getReactions(
                currentUserProvider.getCurrentUsername(),
                messageId
        ));
    }

    @PatchMapping("/{messageId}")
    public ResponseData<MessageResponse> editMessage(
            @Positive @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request
    ) {
        return new ResponseData<>(true, "Edit message successfully", messageService.editMessage(currentUserProvider.getCurrentUsername(), messageId, request));
    }

    @DeleteMapping("/{messageId}")
    public ResponseData<MessageResponse> deleteMessage(@Positive @PathVariable Long messageId) {
        return new ResponseData<>(true, "Delete message successfully", messageService.deleteMessage(currentUserProvider.getCurrentUsername(), messageId));
    }
}
