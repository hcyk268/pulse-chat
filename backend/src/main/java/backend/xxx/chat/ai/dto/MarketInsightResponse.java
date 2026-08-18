package backend.xxx.chat.ai.dto;

import java.time.Instant;
import java.util.List;

public record MarketInsightResponse(
        String symbol,
        String insight,
        List<String> keyPoints,
        List<String> riskNotes,
        Instant generatedAt,
        String model
) {
}