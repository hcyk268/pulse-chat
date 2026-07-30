package backend.xxx.chat.market.service;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.dto.CoinMarketItemResponse;
import backend.xxx.chat.market.dto.MarketCandleResponse;
import backend.xxx.chat.market.dto.OverviewMarketResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.MarketCandleRepository;
import backend.xxx.chat.market.repository.MarketPairRepository;
import backend.xxx.chat.market.repository.MarketTrendingRepository;
import backend.xxx.chat.market.stream.BinanceMarketDataCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @Mock
    private MarketAssetRepository marketAssetRepository;

    @Mock
    private MarketPairRepository marketPairRepository;

    @Mock
    private MarketTrendingRepository marketTrendingRepository;

    @Mock
    private MarketCandleRepository marketCandleRepository;

    @Mock
    private BinanceMarketDataCache binanceMarketDataCache;

    @Mock
    private BinanceStreamProperties binanceStreamProperties;

    @Mock
    private MarketCandleHistoryBackfillService marketCandleHistoryBackfillService;

    @Spy
    private MarketMapper marketMapper = new MarketMapper();

    @InjectMocks
    private MarketService marketService;

    @Test
    void getMarketFiltersOutActivePairsWithoutTickerCache() {
        MarketAsset btc = asset(1L, "BTC", "Bitcoin");
        MarketAsset doge = asset(2L, "DOGE", "Dogecoin");
        MarketPair btcPair = pair(10L, btc, "BTCUSDT");
        MarketPair dogePair = pair(20L, doge, "DOGEUSDT");
        MarketTickerLatestHash btcTicker = new MarketTickerLatestHash();
        btcTicker.setSymbol("BTCUSDT");

        when(marketPairRepository.findActivePairsWithAsset("BINANCE"))
                .thenReturn(List.of(btcPair, dogePair));
        when(binanceMarketDataCache.getTickers(List.of("BTCUSDT", "DOGEUSDT")))
                .thenReturn(List.of(btcTicker));
        when(marketAssetRepository.findAllByActiveTrueOrderByMarketCapRankAsc())
                .thenReturn(List.of(btc, doge));
        when(marketTrendingRepository.findAllByOrderByScoreAsc()).thenReturn(List.of());

        OverviewMarketResponse response = marketService.getMarket();

        assertThat(response.coins())
                .extracting(CoinMarketItemResponse::symbol)
                .containsExactly("BTC");
        assertThat(response.coins().get(0).pairSymbol()).isEqualTo("BTCUSDT");
    }

    @Test
    void getCandlesBackfillsConfiguredIntervalAndReturnsRecentCandles() {
        MarketAsset btc = asset(1L, "BTC", "Bitcoin");
        MarketPair btcPair = pair(10L, btc, "BTCUSDT");

        when(marketPairRepository.findByExchangeAndSymbol("BINANCE", "BTC"))
                .thenReturn(Optional.empty());
        when(marketAssetRepository.findFirstBySymbolIgnoreCaseAndActiveTrue("BTC"))
                .thenReturn(Optional.of(btc));
        when(marketPairRepository.findFirstByAsset_IdAndExchangeAndActiveTrue(1L, "BINANCE"))
                .thenReturn(Optional.of(btcPair));
        when(marketPairRepository.findByExchangeAndSymbol("BINANCE", "BTCUSDT"))
                .thenReturn(Optional.of(btcPair));
        when(binanceStreamProperties.getCandleIntervals()).thenReturn(List.of("4h", "1d", "1w"));
        when(marketCandleRepository.findByPairIdAndIntervalNameOrderByOpenTimeDesc(
                eq(10L),
                eq("4h"),
                any(Pageable.class)
        )).thenReturn(List.of());

        List<MarketCandleResponse> candles = marketService.getCandles("btc", "4H");

        assertThat(candles).isEmpty();
        verify(marketCandleHistoryBackfillService).backfillIfNeeded(btcPair, List.of("4h"));
    }

    private MarketAsset asset(Long id, String symbol, String name) {
        MarketAsset asset = new MarketAsset();
        asset.setId(id);
        asset.setSymbol(symbol);
        asset.setName(name);
        asset.setCoingeckoId(name.toLowerCase());
        asset.setActive(true);
        return asset;
    }

    private MarketPair pair(Long id, MarketAsset asset, String symbol) {
        MarketPair pair = new MarketPair();
        pair.setId(id);
        pair.setAsset(asset);
        pair.setExchange("BINANCE");
        pair.setBaseSymbol(asset.getSymbol());
        pair.setQuoteSymbol("USDT");
        pair.setSymbol(symbol);
        pair.setActive(true);
        return pair;
    }
}





