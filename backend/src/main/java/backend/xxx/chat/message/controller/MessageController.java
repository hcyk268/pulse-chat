package backend.xxx.chat.message.controller;


import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.message.dto.*;
import backend.xxx.chat.message.model.MessageReactionEmoji;
import backend.xxx.chat.message.service.MessageReactionService;
import backend.xxx.chat.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final CurrentUserProvider currentUserProvider;
    private final MessageService messageService;
    private final MessageReactionService messageReactionService;

    @GetMapping()
    public ResponseEntity<MessageHistoryResponse> getHistory(
            @RequestParam(name = "conversationId") Long conversationId,
            @RequestParam(name = "limit", required = false, defaultValue = "20") Short limit,
            @RequestParam(name = "cursor", required = false) String cursor) {
        return ResponseEntity.ok(messageService.getHistory(currentUserProvider.getCurrentUsername(), conversationId, limit, cursor));
    }

    @PostMapping()
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED.value()).body(messageService.sendMessage(currentUserProvider.getCurrentUsername(), request));
    }

    @PostMapping("/read")
    public ResponseEntity<MarkReadResponse> readMessage(@Valid @RequestBody MarkReadRequest request) {
        return ResponseEntity.ok(messageService.readMessage(currentUserProvider.getCurrentUsername(), request));
    }

    @PostMapping("/{messageId}/pin")
    public ResponseEntity<MessagePinResponse> pinMessage(@PathVariable Long messageId) {
        MessageService.PinMessageResult result =
                messageService.pinMessage(currentUserProvider.getCurrentUsername(), messageId);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @DeleteMapping("/{messageId}/pin")
    public ResponseEntity<UnPinMessageResponse> unPinMessage(@PathVariable Long messageId) {
        return ResponseEntity.ok(messageService.unPinMessage(currentUserProvider.getCurrentUsername(), messageId));
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<MessageReactionResponse> reactMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody MessageReactionRequest request
    ) {
        MessageReactionService.ReactMessageResult result = messageReactionService.reactMessage(
                currentUserProvider.getCurrentUsername(),
                messageId,
                request
        );

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @DeleteMapping("/{messageId}/reactions/{emoji}")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long messageId,
            @PathVariable MessageReactionEmoji emoji
    ) {
        messageReactionService.removeReaction(
                currentUserProvider.getCurrentUsername(),
                messageId,
                emoji
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{messageId}/reactions")
    public ResponseEntity<MessageReactionsResponse> getReactions(@PathVariable Long messageId) {
        return ResponseEntity.ok(messageReactionService.getReactions(
                currentUserProvider.getCurrentUsername(),
                messageId
        ));
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request
    ) {
        return ResponseEntity.ok(messageService.editMessage(currentUserProvider.getCurrentUsername(), messageId, request));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<MessageResponse> deleteMessage(@PathVariable Long messageId) {
        return ResponseEntity.ok(messageService.deleteMessage(currentUserProvider.getCurrentUsername(), messageId));
    }

}
