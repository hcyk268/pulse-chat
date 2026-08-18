package backend.xxx.chat.ai.prompt;

public final class InstructionPrompt {
    private InstructionPrompt() {
    }

    public static final String CONVERSATION_SUMMARY_PROMPT = "Summarize the conversation for the current user. Focus on decisions, unresolved questions, "
            + "important context, and action items. Do not invent facts. If there is not enough content, say so briefly.";

    public static final String COMMUNITY_MODERATION_PROMPT = "Moderate the community content and suggest existing category/tag slugs when helpful. "
            + "decision must be ALLOW, REVIEW, or BLOCK. BLOCK only for clearly unsafe content. "
            + "Use REVIEW for ambiguous cases. Suggested tags must come from the provided active tags.";

    public static final String MARKET_INSIGHT_PROMPT = "Explain the market data clearly and briefly. Use recentDailyCandles for recent-days movement when available. "
            + "If requested coin detail or candles are unavailable, say the backend data was not found or unavailable. "
            + "Do not provide financial advice, buy/sell instructions, or guarantees. "
            + "Focus on observable data and risk context.";

}
