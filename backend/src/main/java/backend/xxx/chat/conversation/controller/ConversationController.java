package backend.xxx.chat.conversation.controller;

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

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Validated
public class ConversationController {

    private final CurrentUserProvider currentUserProvider;
    private final ConversationService conversationService;
    private final MessageService messageService;

    @PostMapping("/direct")
    public ResponseEntity<ResponseData<DirectConversationResponse>> createOrOpenDirectConversation(@Valid @RequestBody CreateDirectConversationRequest request) {
        ConversationService.CreateOrOpenDirectConversationResult result =
                conversationService.createOrOpenDirectConversation(currentUserProvider.getCurrentUsername(), request);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created() ? "conversation.direct.create.success" : "conversation.direct.open.success";
        return ResponseEntity.status(status).body(new ResponseData<>(true, message, result.response()));
    }

    @PostMapping("/group")
    public ResponseEntity<ResponseData<ConversationDetailResponse>> createGroupConversation(@Valid @RequestBody CreateGroupConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ResponseData<>(true, "conversation.group.create.success", conversationService.createGroupConversation(currentUserProvider.getCurrentUsername(), request))
        );
    }

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

    @PostMapping("/{conversationId}/invitations/accept")
    public ResponseData<ConversationDetailResponse> acceptGroupInvitation(@Positive @PathVariable Long conversationId) {
        return new ResponseData<>(true, "conversation.group.invitation.accept.success", conversationService.acceptGroupInvitation(
                currentUserProvider.getCurrentUsername(),
                conversationId
        ));
    }

    @PostMapping("/{conversationId}/invitations/reject")
    public ResponseData<Void> rejectGroupInvitation(@Positive @PathVariable Long conversationId) {
        conversationService.rejectGroupInvitation(currentUserProvider.getCurrentUsername(), conversationId);
        return new ResponseData<>(true, "conversation.group.invitation.reject.success");
    }

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

    @PostMapping("/{conversationId}/leave")
    public ResponseData<Void> leaveGroup(@Positive @PathVariable Long conversationId) {
        conversationService.leaveGroup(currentUserProvider.getCurrentUsername(), conversationId);
        return new ResponseData<>(true, "conversation.group.leave.success");
    }

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

    @GetMapping("/{conversationId}")
    public ResponseData<ConversationDetailResponse> getDetailConversation(@Positive @PathVariable Long conversationId) {
        return new ResponseData<>(true, "conversation.detail.success", conversationService.getDetailConversation(conversationId, currentUserProvider.getCurrentUsername()));
    }

    @GetMapping("/{conversationId}/pins")
    public ResponseData<ConversationPinnedMessagesResponse> getPinnedMessages(@Positive @PathVariable Long conversationId) {
        return new ResponseData<>(true, "conversation.pinned.list.success", messageService.getPinnedMessages(
                currentUserProvider.getCurrentUsername(),
                conversationId
        ));
    }
}
