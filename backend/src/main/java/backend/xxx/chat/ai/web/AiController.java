package backend.xxx.chat.ai.web;

import backend.xxx.chat.ai.dto.CommunityModerationRequest;
import backend.xxx.chat.ai.dto.CommunityModerationResponse;
import backend.xxx.chat.ai.dto.ConversationSummaryRequest;
import backend.xxx.chat.ai.dto.ConversationSummaryResponse;
import backend.xxx.chat.ai.dto.MarketInsightRequest;
import backend.xxx.chat.ai.dto.MarketInsightResponse;
import backend.xxx.chat.ai.dto.SmartAssistantRequest;
import backend.xxx.chat.ai.dto.SmartAssistantResponse;
import backend.xxx.chat.ai.usecase.CommunityModerationUseCase;
import backend.xxx.chat.ai.usecase.ConversationSummaryUseCase;
import backend.xxx.chat.ai.usecase.MarketInsightUseCase;
import backend.xxx.chat.ai.usecase.SmartAssistantUseCase;
import backend.xxx.chat.common.dto.ResponseData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "AI orchestration, moderation, summary, and assistant APIs")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Validated
public class AiController {

    private final ConversationSummaryUseCase conversationSummaryUseCase;
    private final CommunityModerationUseCase communityModerationUseCase;
    private final MarketInsightUseCase marketInsightUseCase;
    private final SmartAssistantUseCase smartAssistantUseCase;

    @Operation(summary = "Summarize conversation")
    @PostMapping("/conversations/{conversationId}/summary")
    @AiRateLimit(action = "conversation-summary")
    public ResponseData<ConversationSummaryResponse> summarizeConversation(
            @Positive @PathVariable Long conversationId,
            @Valid @RequestBody(required = false) ConversationSummaryRequest request
    ) {
        return new ResponseData<>(true, "", conversationSummaryUseCase.summarize(conversationId, request));
    }

    @Operation(summary = "Moderate community content")
    @PostMapping("/community/moderation")
    @AiRateLimit(action = "community-moderation")
    public ResponseData<CommunityModerationResponse> moderateCommunityContent(
            @Valid @RequestBody CommunityModerationRequest request
    ) {
        return new ResponseData<>(true, "", communityModerationUseCase.moderate(request));
    }

    @Operation(summary = "Generate market insight")
    @PostMapping("/market/insight")
    @AiRateLimit(action = "market-insight")
    public ResponseData<MarketInsightResponse> generateMarketInsight(
            @Valid @RequestBody(required = false) MarketInsightRequest request
    ) {
        return new ResponseData<>(true, "", marketInsightUseCase.generate(request));
    }

    @Operation(summary = "Ask smart assistant")
    @PostMapping("/assistant")
    @AiRateLimit(action = "assistant")
    public ResponseData<SmartAssistantResponse> askAssistant(
            @Valid @RequestBody SmartAssistantRequest request
    ) {
        return new ResponseData<>(true, "", smartAssistantUseCase.answer(request));
    }
}