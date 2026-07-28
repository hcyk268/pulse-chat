package backend.xxx.chat.market.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.MarketCandle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketCandleRepository extends JpaRepository<MarketCandle, Long> {

    Optional<MarketCandle> findByPairIdAndIntervalNameAndOpenTime(
            Long pairId,
            String intervalName,
            Instant openTime
    );

    long countByPairIdAndIntervalName(Long pairId, String intervalName);

    Optional<MarketCandle> findFirstByPairIdAndIntervalNameOrderByOpenTimeDesc(Long pairId, String intervalName);

    List<MarketCandle> findByPairIdAndIntervalNameOrderByOpenTimeDesc(
            Long pairId,
            String intervalName,
            Pageable pageable
    );
}