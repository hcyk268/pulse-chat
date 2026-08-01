package backend.xxx.chat.market.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.MarketTrending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MarketTrendingRepository extends JpaRepository<MarketTrending, Long> {

    Optional<MarketTrending> findByCoingeckoId(String coingeckoId);

    @Query("""
            from MarketTrending trending
            left join fetch trending.asset
            order by trending.score asc
            """)
    List<MarketTrending> findAllByOrderByScoreAsc();

    List<MarketTrending> findAllByCoingeckoIdIn(Collection<String> coingeckoIds);
}
