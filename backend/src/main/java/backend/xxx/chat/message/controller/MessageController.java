package backend.xxx.chat.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Messages", description = "Message, read receipt, pin, and reaction APIs")
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Validated
public class MessageController {

    private final CurrentUserProvider currentUserProvider;
    private final MessageService messageService;
    private final MessageReactionService messageReactionService;

    @Operation(summary = "Get message history")
    @GetMapping()
    public ResponseData<MessageHistoryResponse> getHistory(
            @Positive @RequestParam(name = "conversationId") Long conversationId,
            @Min(1) @Max(50) @RequestParam(name = "limit", required = false, defaultValue = "20") Short limit,
            @RequestParam(name = "cursor", required = false) String cursor
    ) {
        return new ResponseData<>(true, "message.history.success", messageService.getHistory(currentUserProvider.getCurrentUsernameOrNull(), conversationId, limit, cursor));
    }

    @Operation(summary = "Send message")
    @PostMapping()
    public ResponseEntity<ResponseData<MessageResponse>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseData<>(true, "message.send.success", messageService.sendMessage(currentUserProvider.getCurrentUsername(), request)));
    }

    @Operation(summary = "Mark messages read")
    @PostMapping("/read")
    public ResponseData<MarkReadResponse> readMessage(@Valid @RequestBody MarkReadRequest request) {
        return new ResponseData<>(true, "message.read.success", messageService.readMessage(currentUserProvider.getCurrentUsername(), request));
    }

    @Operation(summary = "Get read receipts")
    @GetMapping("/{messageId}/reads")
    public ResponseData<MessageReadReceiptsResponse> getReadReceipts(@Positive @PathVariable Long messageId) {
        return new ResponseData<>(true, "message.read.receipts.success", messageService.getReadReceipts(
                currentUserProvider.getCurrentUsername(),
                messageId
        ));
    }

    @Operation(summary = "Pin message")
    @PostMapping("/{messageId}/pin")
    public ResponseEntity<ResponseData<MessagePinResponse>> pinMessage(@Positive @PathVariable Long messageId) {
        MessageService.PinMessageResult result =
                messageService.pinMessage(currentUserProvider.getCurrentUsername(), messageId);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created() ? "message.pin.success" : "message.pin.already";
        return ResponseEntity.status(status).body(new ResponseData<>(true, message, result.response()));
    }

    @Operation(summary = "Unpin message")
    @DeleteMapping("/{messageId}/pin")
    public ResponseData<UnPinMessageResponse> unPinMessage(@Positive @PathVariable Long messageId) {
        return new ResponseData<>(true, "message.unpin.success", messageService.unPinMessage(currentUserProvider.getCurrentUsername(), messageId));
    }

    @Operation(summary = "Add or update reaction")
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
        String message = result.created() ? "message.reaction.add.success" : "message.reaction.update.success";
        return ResponseEntity.status(status).body(new ResponseData<>(true, message, result.response()));
    }

    @Operation(summary = "Remove reaction")
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
        return new ResponseData<>(true, "message.reaction.remove.success");
    }

    @Operation(summary = "List reactions")
    @GetMapping("/{messageId}/reactions")
    public ResponseData<MessageReactionsResponse> getReactions(@Positive @PathVariable Long messageId) {
        return new ResponseData<>(true, "message.reaction.list.success", messageReactionService.getReactions(
                currentUserProvider.getCurrentUsername(),
                messageId
        ));
    }

    @Operation(summary = "Edit message")
    @PatchMapping("/{messageId}")
    public ResponseData<MessageResponse> editMessage(
            @Positive @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request
    ) {
        return new ResponseData<>(true, "message.edit.success", messageService.editMessage(currentUserProvider.getCurrentUsername(), messageId, request));
    }

    @Operation(summary = "Delete message")
    @DeleteMapping("/{messageId}")
    public ResponseData<MessageResponse> deleteMessage(@Positive @PathVariable Long messageId) {
        return new ResponseData<>(true, "message.delete.success", messageService.deleteMessage(currentUserProvider.getCurrentUsername(), messageId));
    }
}
