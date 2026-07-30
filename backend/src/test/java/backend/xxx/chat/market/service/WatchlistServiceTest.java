package backend.xxx.chat.market.service;

import java.util.List;

import backend.xxx.chat.market.dto.WatchlistItemResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.UserWatchlistItem;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.UserWatchlistItemRepository;
import backend.xxx.chat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private UserWatchlistItemRepository userWatchlistItemRepository;

    @Mock
    private MarketAssetRepository marketAssetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WatchlistService watchlistService;

    @Test
    void getWatchlistFiltersInactiveAssets() {
        UserWatchlistItem activeItem = item(1L, asset(10L, "BTC", true));
        UserWatchlistItem inactiveItem = item(2L, asset(20L, "DOGE", false));

        when(userWatchlistItemRepository.findAllByUser_UsernameIgnoreCaseOrderByCreatedAtDescIdDesc("alice"))
                .thenReturn(List.of(activeItem, inactiveItem));

        List<WatchlistItemResponse> response = watchlistService.getWatchlist("alice");

        assertThat(response)
                .extracting(item -> item.asset().symbol())
                .containsExactly("BTC");
    }

    private UserWatchlistItem item(Long id, MarketAsset asset) {
        UserWatchlistItem item = new UserWatchlistItem();
        item.setId(id);
        item.setAsset(asset);
        return item;
    }

    private MarketAsset asset(Long id, String symbol, boolean active) {
        MarketAsset asset = new MarketAsset();
        asset.setId(id);
        asset.setCoingeckoId(symbol.toLowerCase());
        asset.setSymbol(symbol);
        asset.setName(symbol);
        asset.setActive(active);
        return asset;
    }
}
