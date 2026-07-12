package backend.xxx.chat.conversation.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddGroupMembersRequest(
        @NotNull @Size(min = 1, max = 100) List<Long> memberIds
) {
}
