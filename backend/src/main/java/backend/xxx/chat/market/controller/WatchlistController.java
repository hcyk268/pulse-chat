package backend.xxx.chat.market.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import backend.xxx.chat.common.dto.ResponseData;
import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.market.dto.WatchlistItemRequest;
import backend.xxx.chat.market.dto.WatchlistItemResponse;
import backend.xxx.chat.market.service.WatchlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Watchlist", description = "User market watchlist APIs")
@RestController
@RequestMapping("/api/v1/market/watchlist")
@RequiredArgsConstructor
@Validated
public class WatchlistController {

    private final CurrentUserProvider currentUserProvider;
    private final WatchlistService watchlistService;

    @Operation(summary = "List watchlist")
    @GetMapping
    public ResponseData<List<WatchlistItemResponse>> getWatchlist() {
        return new ResponseData<>(
                true,
                "market.watchlist.list.success",
                watchlistService.getWatchlist(currentUserProvider.getCurrentUsername())
        );
    }

    @Operation(summary = "Get watchlist item")
    @GetMapping("/{itemId}")
    public ResponseData<WatchlistItemResponse> getWatchlistItem(@Positive @PathVariable Long itemId) {
        return new ResponseData<>(
                true,
                "market.watchlist.detail.success",
                watchlistService.getWatchlistItem(currentUserProvider.getCurrentUsername(), itemId)
        );
    }

    @Operation(summary = "Add watchlist item")
    @PostMapping
    public ResponseEntity<ResponseData<WatchlistItemResponse>> addWatchlistItem(
            @Valid @RequestBody WatchlistItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseData<>(
                true,
                "market.watchlist.add.success",
                watchlistService.addWatchlistItem(currentUserProvider.getCurrentUsername(), request)
        ));
    }

    @Operation(summary = "Update watchlist item")
    @PatchMapping("/{itemId}")
    public ResponseData<WatchlistItemResponse> updateWatchlistItem(
            @Positive @PathVariable Long itemId,
            @Valid @RequestBody WatchlistItemRequest request
    ) {
        return new ResponseData<>(
                true,
                "market.watchlist.update.success",
                watchlistService.updateWatchlistItem(currentUserProvider.getCurrentUsername(), itemId, request)
        );
    }

    @Operation(summary = "Remove watchlist item")
    @DeleteMapping("/{itemId}")
    public ResponseData<Void> removeWatchlistItem(@Positive @PathVariable Long itemId) {
        watchlistService.removeWatchlistItem(currentUserProvider.getCurrentUsername(), itemId);
        return new ResponseData<>(true, "market.watchlist.remove.success");
    }

    @Operation(summary = "Remove watchlist item by symbol")
    @DeleteMapping("/symbols/{symbol}")
    public ResponseData<Void> removeWatchlistItemBySymbol(@NotBlank @PathVariable String symbol) {
        watchlistService.removeWatchlistItemBySymbol(currentUserProvider.getCurrentUsername(), symbol);
        return new ResponseData<>(true, "market.watchlist.remove.success");
    }
}
