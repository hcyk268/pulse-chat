package backend.xxx.chat.market.service;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.config.RedisConfig;
import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.dto.CoinDetailResponse;
import backend.xxx.chat.market.dto.OverviewMarketResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.MarketCandleRepository;
import backend.xxx.chat.market.repository.MarketPairRepository;
import backend.xxx.chat.market.repository.MarketTrendingRepository;
import backend.xxx.chat.market.stream.BinanceMarketDataCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(MarketServiceCacheTest.CacheTestConfiguration.class)
class MarketServiceCacheTest {

    private final MarketService marketService;
    private final MarketAssetRepository marketAssetRepository;
    private final MarketPairRepository marketPairRepository;
    private final MarketTrendingRepository marketTrendingRepository;
    private final BinanceMarketDataCache binanceMarketDataCache;

    @Autowired
    MarketServiceCacheTest(
            MarketService marketService,
            MarketAssetRepository marketAssetRepository,
            MarketPairRepository marketPairRepository,
            MarketTrendingRepository marketTrendingRepository,
            BinanceMarketDataCache binanceMarketDataCache
    ) {
        this.marketService = marketService;
        this.marketAssetRepository = marketAssetRepository;
        this.marketPairRepository = marketPairRepository;
        this.marketTrendingRepository = marketTrendingRepository;
        this.binanceMarketDataCache = binanceMarketDataCache;
    }

    @BeforeEach
    void resetMocks() {
        reset(marketAssetRepository, marketPairRepository, marketTrendingRepository, binanceMarketDataCache);
    }

    @Test
    void cachesMarketOverview() {
        MarketAsset btc = asset(1L, "BTC", "Bitcoin");
        MarketPair btcPair = pair(10L, btc, "BTCUSDT");
        MarketTickerLatestHash btcTicker = new MarketTickerLatestHash();
        btcTicker.setSymbol("BTCUSDT");

        when(marketPairRepository.findActivePairsWithAsset("BINANCE")).thenReturn(List.of(btcPair));
        when(binanceMarketDataCache.getTickers(List.of("BTCUSDT"))).thenReturn(List.of(btcTicker));
        when(marketAssetRepository.findAllByActiveTrueOrderByMarketCapRankAsc()).thenReturn(List.of(btc));
        when(marketTrendingRepository.findAllByOrderByScoreAsc()).thenReturn(List.of());

        OverviewMarketResponse firstResponse = marketService.getMarket();
        OverviewMarketResponse secondResponse = marketService.getMarket();

        assertThat(secondResponse).isSameAs(firstResponse);
        assertThat(secondResponse.coins()).hasSize(1);
        verify(marketPairRepository, times(1)).findActivePairsWithAsset("BINANCE");
        verify(binanceMarketDataCache, times(1)).getTickers(List.of("BTCUSDT"));
        verify(marketAssetRepository, times(1)).findAllByActiveTrueOrderByMarketCapRankAsc();
        verify(marketTrendingRepository, times(1)).findAllByOrderByScoreAsc();
    }

    @Test
    void cachesCoinDetailByNormalizedSymbol() {
        MarketAsset btc = asset(1L, "BTC", "Bitcoin");
        MarketPair btcPair = pair(10L, btc, "BTCUSDT");

        when(marketAssetRepository.findFirstBySymbolIgnoreCaseAndActiveTrue("BTC"))
                .thenReturn(Optional.of(btc));
        when(marketPairRepository.findFirstByAsset_IdAndExchangeAndActiveTrue(1L, "BINANCE"))
                .thenReturn(Optional.of(btcPair));

        CoinDetailResponse firstResponse = marketService.getCoinDetail("btc");
        CoinDetailResponse secondResponse = marketService.getCoinDetail(" BTC ");

        assertThat(secondResponse).isSameAs(firstResponse);
        assertThat(secondResponse.symbol()).isEqualTo("BTC");
        verify(marketAssetRepository, times(1)).findFirstBySymbolIgnoreCaseAndActiveTrue("BTC");
        verify(marketPairRepository, times(1)).findFirstByAsset_IdAndExchangeAndActiveTrue(1L, "BINANCE");
    }

    private static MarketAsset asset(Long id, String symbol, String name) {
        MarketAsset asset = new MarketAsset();
        asset.setId(id);
        asset.setSymbol(symbol);
        asset.setName(name);
        asset.setCoingeckoId(name.toLowerCase());
        asset.setActive(true);
        return asset;
    }

    private static MarketPair pair(Long id, MarketAsset asset, String symbol) {
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

    @Configuration
    @EnableCaching
    static class CacheTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    RedisConfig.MARKET_OVERVIEW_CACHE,
                    RedisConfig.MARKET_COIN_DETAIL_CACHE
            );
        }

        @Bean
        MarketService marketService(
                MarketAssetRepository marketAssetRepository,
                MarketPairRepository marketPairRepository,
                MarketTrendingRepository marketTrendingRepository,
                MarketCandleRepository marketCandleRepository,
                BinanceMarketDataCache binanceMarketDataCache,
                BinanceStreamProperties binanceStreamProperties,
                MarketCandleHistoryBackfillService marketCandleHistoryBackfillService,
                MarketMapper marketMapper
        ) {
            return new MarketService(
                    marketAssetRepository,
                    marketPairRepository,
                    marketTrendingRepository,
                    marketCandleRepository,
                    binanceMarketDataCache,
                    binanceStreamProperties,
                    marketCandleHistoryBackfillService,
                    marketMapper
            );
        }

        @Bean
        MarketMapper marketMapper() {
            return new MarketMapper();
        }

        @Bean
        MarketAssetRepository marketAssetRepository() {
            return mock(MarketAssetRepository.class);
        }

        @Bean
        MarketPairRepository marketPairRepository() {
            return mock(MarketPairRepository.class);
        }

        @Bean
        MarketTrendingRepository marketTrendingRepository() {
            return mock(MarketTrendingRepository.class);
        }

        @Bean
        MarketCandleRepository marketCandleRepository() {
            return mock(MarketCandleRepository.class);
        }

        @Bean
        BinanceMarketDataCache binanceMarketDataCache() {
            return mock(BinanceMarketDataCache.class);
        }

        @Bean
        BinanceStreamProperties binanceStreamProperties() {
            return mock(BinanceStreamProperties.class);
        }

        @Bean
        MarketCandleHistoryBackfillService marketCandleHistoryBackfillService() {
            return mock(MarketCandleHistoryBackfillService.class);
        }
    }
}