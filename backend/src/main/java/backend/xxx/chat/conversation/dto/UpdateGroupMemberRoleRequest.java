package backend.xxx.chat.conversation.dto;

import backend.xxx.chat.conversation.model.ParticipantRole;
import jakarta.validation.constraints.NotNull;

public record UpdateGroupMemberRoleRequest(
        @NotNull ParticipantRole role
) {
}
