package backend.xxx.chat.market.service;

import java.util.List;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.market.dto.CoinDetailResponse;
import backend.xxx.chat.market.dto.OverviewMarketResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.model.MarketTrending;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.MarketPairRepository;
import backend.xxx.chat.market.repository.MarketTrendingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketService {

    private static final String BINANCE_EXCHANGE = "BINANCE";

    private final MarketAssetRepository marketAssetRepository;
    private final MarketPairRepository marketPairRepository;
    private final MarketTrendingRepository marketTrendingRepository;

    @Transactional(readOnly = true)
    public OverviewMarketResponse getMarket() {
        List<OverviewMarketResponse.CoinMarketItemResponse> coins = marketAssetRepository
                .findAllByActiveTrueOrderByMarketCapRankAsc()
                .stream()
                .map(this::toMarketItemResponse)
                .toList();
        List<OverviewMarketResponse.TrendingCoinResponse> trending = marketTrendingRepository
                .findAllByOrderByScoreAsc()
                .stream()
                .map(this::toTrendingResponse)
                .toList();

        return new OverviewMarketResponse(coins, trending);
    }

    @Transactional(readOnly = true)
    public CoinDetailResponse getCoinDetail(String symbol) {
        MarketAsset asset = marketAssetRepository.findFirstBySymbolIgnoreCaseAndActiveTrue(symbol)
                .orElseThrow(() -> new NotFoundException("market.coin.not.found"));
        CoinDetailResponse.MarketPairResponse binancePair = marketPairRepository
                .findFirstByAsset_IdAndExchangeAndActiveTrue(asset.getId(), BINANCE_EXCHANGE)
                .map(this::toPairResponse)
                .orElse(null);

        return new CoinDetailResponse(
                asset.getId(),
                asset.getCoingeckoId(),
                asset.getSymbol(),
                asset.getName(),
                asset.getImageUrl(),
                asset.getMarketCapRank(),
                asset.getCurrentPriceUsd(),
                asset.getPriceChangePercentage24h(),
                asset.getHigh24h(),
                asset.getLow24h(),
                asset.getMarketCap(),
                asset.getTotalVolume(),
                asset.getCirculatingSupply(),
                asset.getTotalSupply(),
                asset.getMaxSupply(),
                asset.getLastSyncedAt(),
                binancePair
        );
    }

    private OverviewMarketResponse.CoinMarketItemResponse toMarketItemResponse(MarketAsset asset) {
        return new OverviewMarketResponse.CoinMarketItemResponse(
                asset.getId(),
                asset.getCoingeckoId(),
                asset.getSymbol(),
                asset.getName(),
                asset.getImageUrl(),
                asset.getMarketCapRank(),
                asset.getCurrentPriceUsd(),
                asset.getPriceChangePercentage24h(),
                asset.getHigh24h(),
                asset.getLow24h(),
                asset.getMarketCap(),
                asset.getTotalVolume(),
                asset.getLastSyncedAt()
        );
    }

    private OverviewMarketResponse.TrendingCoinResponse toTrendingResponse(MarketTrending trending) {
        MarketAsset asset = trending.getAsset();
        return new OverviewMarketResponse.TrendingCoinResponse(
                trending.getId(),
                asset == null ? null : asset.getId(),
                trending.getCoingeckoId(),
                trending.getSymbol(),
                trending.getName(),
                trending.getThumbUrl(),
                trending.getMarketCapRank(),
                trending.getScore(),
                trending.getSnapshotAt()
        );
    }

    private CoinDetailResponse.MarketPairResponse toPairResponse(MarketPair pair) {
        return new CoinDetailResponse.MarketPairResponse(
                pair.getId(),
                pair.getExchange(),
                pair.getBaseSymbol(),
                pair.getQuoteSymbol(),
                pair.getSymbol(),
                pair.getLastSyncedAt()
        );
    }
}