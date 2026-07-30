package backend.xxx.chat.market.client.dto;

import java.util.List;

public record BinanceExchangeInfoResponse(
        List<BinanceSymbolResponse> symbols
) {
}
