package backend.xxx.chat.ai.tool;

import backend.xxx.chat.ai.orchestration.AiUseCaseType;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AiToolPolicy {

    private static final Set<String> SMART_ASSISTANT_TOOLS = Set.of(
            "getCurrentUser",
            "getConversationMessages",
            "getPinnedMessages",
            "getMarket",
            "getCoinDetail",
            "getTickerCandles",
            "getCommunityDetail",
            "searchMessagesByKeyword"
    );

    private static final Map<AiUseCaseType, Set<String>> ALLOWED_TOOLS_BY_USE_CASE = Map.of(
            AiUseCaseType.SMART_ASSISTANT, SMART_ASSISTANT_TOOLS
    );

    private static final Map<String, String> OUTPUT_SCOPES = Map.of(
            "getCurrentUser", "id, username, displayName, avatarUrl, bio, accountStatus, emailVerified; no email or tokens",
            "getConversationMessages", "message id, sender id/name, content, type, timestamps, attachment count only",
            "getPinnedMessages", "pin id, message id, sender id/name, content, type, pinned/edited timestamps only",
            "getMarket", "public market overview and trending coin data",
            "getCoinDetail", "public coin detail and 24h market snapshot",
            "getTickerCandles", "public OHLCV candle rows for recent market movement",
            "getCommunityDetail", "community detail visible to current user through existing service permissions",
            "searchMessagesByKeyword", "matching message id, sender id/name, content, createdAt inside an allowed conversation"
    );

    public boolean isAllowed(AiUseCaseType useCase, String toolName) {
        return allowedToolNames(useCase).contains(toolName);
    }

    public Set<String> allowedToolNames(AiUseCaseType useCase) {
        return ALLOWED_TOOLS_BY_USE_CASE.getOrDefault(useCase, Set.of());
    }

    public String outputScope(String toolName) {
        return OUTPUT_SCOPES.getOrDefault(toolName, "read-only backend data scoped by tool implementation");
    }
}