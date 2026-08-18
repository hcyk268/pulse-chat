package backend.xxx.chat.ai.tool.impl;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.tool.AiTool;
import backend.xxx.chat.ai.tool.AiToolAccess;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.community.dto.CommunityDetailResponse;
import backend.xxx.chat.community.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCommunityDetailTool implements AiTool<GetCommunityDetailTool.Input, CommunityDetailResponse> {

    private final CommunityService communityService;

    @Override
    public String name() {
        return "getCommunityDetail";
    }

    @Override
    public String description() {
        return "Return community detail by slug when the current user can view it.";
    }

    @Override
    public String argumentSchema() {
        return "{\"slug\": string}";
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public AiToolAccess access() {
        return AiToolAccess.READ_ONLY;
    }

    @Override
    public CommunityDetailResponse execute(Input input, AiExecutionContext context) {
        if (input.slug() == null || input.slug().isBlank()) {
            throw new ValidationException("communitySlug must not be blank");
        }
        return communityService.getCommunityDetail(context.currentUsername(), input.slug());
    }

    public record Input(String slug) {
    }
}