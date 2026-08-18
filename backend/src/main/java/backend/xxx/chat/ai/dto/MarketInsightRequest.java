package backend.xxx.chat.ai.dto;

import jakarta.validation.constraints.Size;

public record MarketInsightRequest(
        @Size(max = 30) String symbol
) {
}