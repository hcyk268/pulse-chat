package backend.xxx.chat.market.scheduler;

import java.util.List;
import java.util.Set;

import backend.xxx.chat.market.client.BinanceExchangeInfoClient;
import backend.xxx.chat.market.client.CoinGeckoClient;
import backend.xxx.chat.market.client.dto.CoinMarketResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.MarketPairRepository;
import backend.xxx.chat.market.repository.MarketTrendingRepository;
import backend.xxx.chat.market.stream.BinanceMarketDataCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoinGeckoSyncServiceTest {

    @Mock
    private CoinGeckoClient coinGeckoClient;

    @Mock
    private BinanceExchangeInfoClient binanceExchangeInfoClient;

    @Mock
    private MarketAssetRepository marketAssetRepository;

    @Mock
    private MarketPairRepository marketPairRepository;

    @Mock
    private MarketTrendingRepository marketTrendingRepository;

    @Mock
    private BinanceMarketDataCache binanceMarketDataCache;

    @InjectMocks
    private CoinGeckoSyncService coinGeckoSyncService;

    @Test
    @SuppressWarnings("unchecked")
    void syncTopMarketsOnlyKeepsBinanceSupportedAssetsAndPairsActive() {
        MarketPair unsupportedExistingPair = new MarketPair();
        unsupportedExistingPair.setExchange("BINANCE");
        unsupportedExistingPair.setBaseSymbol("DOGE");
        unsupportedExistingPair.setQuoteSymbol("USDT");
        unsupportedExistingPair.setSymbol("DOGEUSDT");
        unsupportedExistingPair.setActive(true);

        when(coinGeckoClient.getTopMarkets()).thenReturn(List.of(
                coin("bitcoin", "btc", "Bitcoin"),
                coin("dogecoin", "doge", "Dogecoin")
        ));
        when(binanceExchangeInfoClient.getTradingSpotSymbolsByQuote("USDT")).thenReturn(Set.of("BTCUSDT"));
        when(marketAssetRepository.findAllByCoingeckoIdIn(anyCollection())).thenReturn(List.of());
        when(marketAssetRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(marketPairRepository.findAllByExchangeAndSymbolIn("BINANCE", Set.of("BTCUSDT", "DOGEUSDT")))
                .thenReturn(List.of(unsupportedExistingPair));
        when(marketPairRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int syncedCount = coinGeckoSyncService.syncTopMarkets();

        assertThat(syncedCount).isEqualTo(2);
        ArgumentCaptor<List<MarketAsset>> assetsCaptor = ArgumentCaptor.forClass(List.class);
        verify(marketAssetRepository).saveAll(assetsCaptor.capture());
        assertThat(assetsCaptor.getValue())
                .filteredOn(asset -> asset.getSymbol().equals("BTC"))
                .allMatch(MarketAsset::isActive);
        assertThat(assetsCaptor.getValue())
                .filteredOn(asset -> asset.getSymbol().equals("DOGE"))
                .noneMatch(MarketAsset::isActive);

        ArgumentCaptor<List<MarketPair>> pairsCaptor = ArgumentCaptor.forClass(List.class);
        verify(marketPairRepository).saveAll(pairsCaptor.capture());
        assertThat(pairsCaptor.getValue())
                .extracting(MarketPair::getSymbol)
                .containsExactlyInAnyOrder("BTCUSDT", "DOGEUSDT");
        assertThat(pairsCaptor.getValue())
                .filteredOn(pair -> pair.getSymbol().equals("BTCUSDT"))
                .allMatch(MarketPair::isActive);
        assertThat(unsupportedExistingPair.isActive()).isFalse();
        verify(binanceMarketDataCache).cacheTickerSymbols(List.of("BTCUSDT"));
    }

    private CoinMarketResponse coin(String id, String symbol, String name) {
        return new CoinMarketResponse(
                id,
                symbol,
                name,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
