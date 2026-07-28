package backend.xxx.chat.community.dto;

import backend.xxx.chat.community.model.CommunityChannelType;
import jakarta.validation.constraints.Size;

public record CreateCommunityChannelRequest(
        @Size(max = 100) String name,
        @Size(max = 500) String description,
        CommunityChannelType type,
        Boolean readOnly
) {
}
