package backend.xxx.chat.market.client.dto;

public record BinanceSymbolResponse(
        String symbol,
        String status,
        String baseAsset,
        String quoteAsset,
        Boolean isSpotTradingAllowed
) {
}
