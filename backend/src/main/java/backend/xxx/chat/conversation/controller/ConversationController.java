package backend.xxx.chat.conversation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import backend.xxx.chat.common.dto.ResponseData;
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

@Tag(name = "Conversations", description = "Direct and group conversation APIs")
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Validated
public class ConversationController {

    private final CurrentUserProvider currentUserProvider;
    private final ConversationService conversationService;
    private final MessageService messageService;

    @Operation(summary = "Create or open direct conversation")
    @PostMapping("/direct")
    public ResponseEntity<ResponseData<DirectConversationResponse>> createOrOpenDirectConversation(@Valid @RequestBody CreateDirectConversationRequest request) {
        ConversationService.CreateOrOpenDirectConversationResult result =
                conversationService.createOrOpenDirectConversation(currentUserProvider.getCurrentUsername(), request);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created() ? "conversation.direct.create.success" : "conversation.direct.open.success";
        return ResponseEntity.status(status).body(new ResponseData<>(true, message, result.response()));
    }

    @Operation(summary = "Create group conversation")
    @PostMapping("/group")
    public ResponseEntity<ResponseData<ConversationDetailResponse>> createGroupConversation(@Valid @RequestBody CreateGroupConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ResponseData<>(true, "conversation.group.create.success", conversationService.createGroupConversation(currentUserProvider.getCurrentUsername(), request))
        );
    }

    @Operation(summary = "Invite group members")
    @PostMapping("/{conversationId}/members")
    public ResponseData<ConversationDetailResponse> inviteGroupMembers(
            @Positive @PathVariable Long conversationId,
            @Valid @RequestBody AddGroupMembersRequest request
    ) {
        return new ResponseData<>(true, "conversation.group.invite.success", conversationService.inviteGroupMembers(
                currentUserProvider.getCurrentUsername(),
                conversationId,
                request
        ));
    }

    @Operation(summary = "Accept group invitation")
    @PostMapping("/{conversationId}/invitations/accept")
    public ResponseData<ConversationDetailResponse> acceptGroupInvitation(@Positive @PathVariable Long conversationId) {
        return new ResponseData<>(true, "conversation.group.invitation.accept.success", conversationService.acceptGroupInvitation(
                currentUserProvider.getCurrentUsername(),
                conversationId
        ));
    }

    @Operation(summary = "Reject group invitation")
    @PostMapping("/{conversationId}/invitations/reject")
    public ResponseData<Void> rejectGroupInvitation(@Positive @PathVariable Long conversationId) {
        conversationService.rejectGroupInvitation(currentUserProvider.getCurrentUsername(), conversationId);
        return new ResponseData<>(true, "conversation.group.invitation.reject.success");
    }

    @Operation(summary = "Remove group member")
    @DeleteMapping("/{conversationId}/members/{memberId}")
    public ResponseData<ConversationDetailResponse> removeGroupMember(
            @Positive @PathVariable Long conversationId,
            @Positive @PathVariable Long memberId
    ) {
        return new ResponseData<>(true, "conversation.group.member.remove.success", conversationService.removeGroupMember(
                currentUserProvider.getCurrentUsername(),
                conversationId,
                memberId
        ));
    }

    @Operation(summary = "Leave group conversation")
    @PostMapping("/{conversationId}/leave")
    public ResponseData<Void> leaveGroup(@Positive @PathVariable Long conversationId) {
        conversationService.leaveGroup(currentUserProvider.getCurrentUsername(), conversationId);
        return new ResponseData<>(true, "conversation.group.leave.success");
    }

    @Operation(summary = "Update group profile")
    @PatchMapping("/{conversationId}/group-profile")
    public ResponseData<ConversationDetailResponse> updateGroupProfile(
            @Positive @PathVariable Long conversationId,
            @Valid @RequestBody UpdateGroupProfileRequest request
    ) {
        return new ResponseData<>(true, "conversation.group.profile.update.success", conversationService.updateGroupProfile(
                currentUserProvider.getCurrentUsername(),
                conversationId,
                request
        ));
    }

    @Operation(summary = "Update group member role")
    @PatchMapping("/{conversationId}/members/{memberId}/role")
    public ResponseData<ConversationDetailResponse> updateGroupMemberRole(
            @Positive @PathVariable Long conversationId,
            @Positive @PathVariable Long memberId,
            @Valid @RequestBody UpdateGroupMemberRoleRequest request
    ) {
        return new ResponseData<>(true, "conversation.group.member.role.update.success", conversationService.updateGroupMemberRole(
                currentUserProvider.getCurrentUsername(),
                conversationId,
                memberId,
                request
        ));
    }

    @Operation(summary = "List conversations")
    @GetMapping
    public ResponseData<ConversationBoxResponse> getConversations(
            @Min(1) @Max(50) @RequestParam(name = "limit", required = false, defaultValue = "20") Short limit,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "snapshotAt", required = false) Instant snapshotAt
    ) {
        return new ResponseData<>(true, "conversation.list.success", conversationService.getConversations(
                limit,
                cursor,
                snapshotAt,
                currentUserProvider.getCurrentUsername()
        ));
    }

    @Operation(summary = "Get conversation detail")
    @GetMapping("/{conversationId}")
    public ResponseData<ConversationDetailResponse> getDetailConversation(@Positive @PathVariable Long conversationId) {
        return new ResponseData<>(true, "conversation.detail.success", conversationService.getDetailConversation(conversationId, currentUserProvider.getCurrentUsername()));
    }

    @Operation(summary = "List pinned messages")
    @GetMapping("/{conversationId}/pins")
    public ResponseData<ConversationPinnedMessagesResponse> getPinnedMessages(@Positive @PathVariable Long conversationId) {
        return new ResponseData<>(true, "conversation.pinned.list.success", messageService.getPinnedMessages(
                currentUserProvider.getCurrentUsername(),
                conversationId
        ));
    }
}
