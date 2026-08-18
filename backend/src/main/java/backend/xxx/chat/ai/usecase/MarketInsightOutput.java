package backend.xxx.chat.ai.usecase;

import java.util.List;

public record MarketInsightOutput(
        String insight,
        List<String> keyPoints,
        List<String> riskNotes
) {
}