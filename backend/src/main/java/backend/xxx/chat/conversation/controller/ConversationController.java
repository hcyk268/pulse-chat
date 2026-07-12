package backend.xxx.chat.conversation.controller;

import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.conversation.dto.*;
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

    @PostMapping("/group")
    public ResponseEntity<ConversationDetailResponse> createGroupConversation(@Valid @RequestBody CreateGroupConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                conversationService.createGroupConversation(currentUserProvider.getCurrentUsername(), request)
        );
    }

    @PostMapping("/{conversationId}/members")
    public ResponseEntity<ConversationDetailResponse> inviteGroupMembers(
            @Positive @PathVariable Long conversationId,
            @Valid @RequestBody AddGroupMembersRequest request
    ) {
        return ResponseEntity.ok(conversationService.inviteGroupMembers(
                currentUserProvider.getCurrentUsername(),
                conversationId,
                request
        ));
    }

    @PostMapping("/{conversationId}/invitations/accept")
    public ResponseEntity<ConversationDetailResponse> acceptGroupInvitation(@Positive @PathVariable Long conversationId) {
        return ResponseEntity.ok(conversationService.acceptGroupInvitation(
                currentUserProvider.getCurrentUsername(),
                conversationId
        ));
    }

    @PostMapping("/{conversationId}/invitations/reject")
    public ResponseEntity<Void> rejectGroupInvitation(@Positive @PathVariable Long conversationId) {
        conversationService.rejectGroupInvitation(currentUserProvider.getCurrentUsername(), conversationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conversationId}/members/{memberId}")
    public ResponseEntity<ConversationDetailResponse> removeGroupMember(
            @Positive @PathVariable Long conversationId,
            @Positive @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(conversationService.removeGroupMember(
                currentUserProvider.getCurrentUsername(),
                conversationId,
                memberId
        ));
    }

    @PostMapping("/{conversationId}/leave")
    public ResponseEntity<Void> leaveGroup(@Positive @PathVariable Long conversationId) {
        conversationService.leaveGroup(currentUserProvider.getCurrentUsername(), conversationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{conversationId}/group-profile")
    public ResponseEntity<ConversationDetailResponse> updateGroupProfile(
            @Positive @PathVariable Long conversationId,
            @Valid @RequestBody UpdateGroupProfileRequest request
    ) {
        return ResponseEntity.ok(conversationService.updateGroupProfile(
                currentUserProvider.getCurrentUsername(),
                conversationId,
                request
        ));
    }

    @PatchMapping("/{conversationId}/members/{memberId}/role")
    public ResponseEntity<ConversationDetailResponse> updateGroupMemberRole(
            @Positive @PathVariable Long conversationId,
            @Positive @PathVariable Long memberId,
            @Valid @RequestBody UpdateGroupMemberRoleRequest request
    ) {
        return ResponseEntity.ok(conversationService.updateGroupMemberRole(
                currentUserProvider.getCurrentUsername(),
                conversationId,
                memberId,
                request
        ));
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
    public ResponseEntity<ConversationDetailResponse> getDetailConversation(@Positive @PathVariable Long conversationId) {
        return ResponseEntity.ok(conversationService.getDetailConversation(conversationId, currentUserProvider.getCurrentUsername()));
    }

    @GetMapping("/{conversationId}/pins")
    public ResponseEntity<ConversationPinnedMessagesResponse> getPinnedMessages(@Positive @PathVariable Long conversationId) {
        return ResponseEntity.ok(messageService.getPinnedMessages(
                currentUserProvider.getCurrentUsername(),
                conversationId
        ));
    }

}