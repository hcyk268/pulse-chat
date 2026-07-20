package backend.xxx.chat.market.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.MarketTrending;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketTrendingRepository extends JpaRepository<MarketTrending, Long> {

    Optional<MarketTrending> findByCoingeckoId(String coingeckoId);

    List<MarketTrending> findAllByOrderByScoreAsc();

    List<MarketTrending> findAllByCoingeckoIdIn(Collection<String> coingeckoIds);
}