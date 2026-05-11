package backend.xxx.chat.conversation.controller;

import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.conversation.dto.ConversationBoxResponse;
import backend.xxx.chat.conversation.dto.CreateDirectConversationRequest;
import backend.xxx.chat.conversation.dto.DirectConversationResponse;
import backend.xxx.chat.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final CurrentUserProvider currentUserProvider;
    private final ConversationService conversationService;

    @PostMapping("/direct")
    public ResponseEntity<DirectConversationResponse> createOrOpenDirectConversation(@Valid @RequestBody CreateDirectConversationRequest request) {
        ConversationService.CreateOrOpenDirectConversationResult result =
                conversationService.createOrOpenDirectConversation(currentUserProvider.getCurrentUsername(), request);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping
    public ResponseEntity<ConversationBoxResponse> getConversations(
            @RequestParam(name = "limit", required = false, defaultValue = "20") Short limit,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "snapshotAt", required = false) Instant snapshotAt
            ){
        return ResponseEntity.ok(
                conversationService.getConversations(
                        limit, cursor, snapshotAt, currentUserProvider.getCurrentUsername()));
    }

}
