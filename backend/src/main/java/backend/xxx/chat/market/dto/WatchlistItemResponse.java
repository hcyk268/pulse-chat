package backend.xxx.chat.market.dto;

import java.time.Instant;

public record WatchlistItemResponse(
        Long id,
        WatchlistAssetResponse asset,
        Instant createdAt,
        Instant updatedAt
) {
}
