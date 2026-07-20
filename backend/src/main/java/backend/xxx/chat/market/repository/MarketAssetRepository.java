package backend.xxx.chat.market.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.MarketAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketAssetRepository extends JpaRepository<MarketAsset, Long> {

    Optional<MarketAsset> findByCoingeckoId(String coingeckoId);

    Optional<MarketAsset> findFirstBySymbolIgnoreCaseAndActiveTrue(String symbol);

    List<MarketAsset> findAllByActiveTrueOrderByMarketCapRankAsc();

    List<MarketAsset> findAllByCoingeckoIdIn(Collection<String> coingeckoIds);
}