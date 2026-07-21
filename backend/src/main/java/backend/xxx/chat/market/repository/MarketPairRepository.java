package backend.xxx.chat.market.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.MarketPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketPairRepository extends JpaRepository<MarketPair, Long> {

    Optional<MarketPair> findByExchangeAndSymbol(String exchange, String symbol);

    Optional<MarketPair> findFirstByAsset_IdAndExchangeAndActiveTrue(Long assetId, String exchange);

    List<MarketPair> findAllByExchangeAndSymbolIn(String exchange, Collection<String> symbols);

    @Query("""
            select pair
            from MarketPair pair
            join fetch pair.asset asset
            where pair.exchange = :exchange
              and pair.active = true
              and asset.active = true
            order by asset.marketCapRank asc
            """)
    List<MarketPair> findActivePairsWithAsset(@Param("exchange") String exchange);
}