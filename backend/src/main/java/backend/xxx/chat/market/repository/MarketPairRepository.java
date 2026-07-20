package backend.xxx.chat.market.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.MarketPair;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketPairRepository extends JpaRepository<MarketPair, Long> {

    Optional<MarketPair> findByExchangeAndSymbol(String exchange, String symbol);

    Optional<MarketPair> findFirstByAsset_IdAndExchangeAndActiveTrue(Long assetId, String exchange);

    List<MarketPair> findAllByExchangeAndSymbolIn(String exchange, Collection<String> symbols);
}