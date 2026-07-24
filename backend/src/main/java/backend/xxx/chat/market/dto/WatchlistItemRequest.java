package backend.xxx.chat.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WatchlistItemRequest(
        @NotBlank(message = "market.watchlist.symbol.blank")
        @Size(max = 20, message = "market.watchlist.symbol.max.length")
        String symbol
) {
}