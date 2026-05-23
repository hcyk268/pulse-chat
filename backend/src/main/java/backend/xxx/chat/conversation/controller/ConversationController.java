package backend.xxx.chat.conversation.controller;

import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.conversation.dto.ConversationBoxResponse;
import backend.xxx.chat.conversation.dto.ConversationPinsResponse;
import backend.xxx.chat.conversation.dto.CreateDirectConversationRequest;
import backend.xxx.chat.conversation.dto.DirectConversationResponse;
import backend.xxx.chat.conversation.service.ConversationService;
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

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Validated
public class ConversationController {

    private final CurrentUserProvider currentUserProvider;
    private final ConversationService conversationService;
    private final MessageService messageService;

    @PostMapping("/direct")
    public ResponseEntity<DirectConversationResponse> createOrOpenDirectConversation(@Valid @RequestBody CreateDirectConversationRequest request) {
        ConversationService.CreateOrOpenDirectConversationResult result =
                conversationService.createOrOpenDirectConversation(currentUserProvider.getCurrentUsername(), request);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping
    public ResponseEntity<ConversationBoxResponse> getConversations(
            @Min(1) @Max(50) @RequestParam(name = "limit", required = false, defaultValue = "20") Short limit,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "snapshotAt", required = false) Instant snapshotAt
            ){
        return ResponseEntity.ok(
                conversationService.getConversations(
                        limit, cursor, snapshotAt, currentUserProvider.getCurrentUsername()));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<DirectConversationResponse> getDetailConversation(@Positive @PathVariable Long conversationId) {
        return ResponseEntity.ok(conversationService.getDetailConversation(conversationId, currentUserProvider.getCurrentUsername()));
    }

    @GetMapping("/{conversationId}/pins")
    public ResponseEntity<ConversationPinsResponse> getConversationPins(@Positive @PathVariable Long conversationId) {
        return ResponseEntity.ok(messageService.getConversationPins(
                currentUserProvider.getCurrentUsername(),
                conversationId
        ));
    }

}
