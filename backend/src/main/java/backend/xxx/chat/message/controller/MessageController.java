package backend.xxx.chat.message.controller;


import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.message.dto.MarkReadRequest;
import backend.xxx.chat.message.dto.MarkReadResponse;
import backend.xxx.chat.message.dto.MessageHistoryResponse;
import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.dto.SendMessageRequest;
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

}
