package backend.xxx.chat.market.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.MarketCandle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketCandleRepository extends JpaRepository<MarketCandle, Long> {

    @Query("""
            select candle.intervalName as intervalName,
                   count(candle) as candleCount,
                   max(candle.closeTime) as latestCloseTime
            from MarketCandle candle
            where candle.pair.id = :pairId
                and candle.intervalName in :intervalNames
            group by candle.intervalName
            """)
    List<CandleHistoryState> findHistoryStates(
            @Param("pairId") Long pairId,
            @Param("intervalNames") Collection<String> intervalNames
    );

    @Query("""
            from MarketCandle candle
            where candle.pair.id = :pairId
                and candle.intervalName in :intervalNames
                and candle.openTime in :openTimes
            """)
    List<MarketCandle> findExistingCandles(
            @Param("pairId") Long pairId,
            @Param("intervalNames") Collection<String> intervalNames,
            @Param("openTimes") Collection<Instant> openTimes
    );

    @Query(value = """
            select ranked.id,
                   ranked.created_at,
                   ranked.updated_at,
                   ranked.pair_id,
                   ranked.interval_name,
                   ranked.open_time,
                   ranked.close_time,
                   ranked.open,
                   ranked.high,
                   ranked.low,
                   ranked.close,
                   ranked.volume,
                   ranked.quote_volume,
                   ranked.trade_count,
                   ranked.is_closed
            from (
                select candle.*,
                       row_number() over (
                           partition by candle.interval_name
                           order by candle.open_time desc, candle.id desc
                       ) as row_number
                from market_candles candle
                where candle.pair_id = :pairId
                    and candle.interval_name in (:intervalNames)
            ) ranked
            where ranked.row_number <= :historyLimit
            order by ranked.interval_name asc, ranked.open_time desc, ranked.id desc
            """, nativeQuery = true)
    List<MarketCandle> findRecentByPairIdAndIntervalNameIn(
            @Param("pairId") Long pairId,
            @Param("intervalNames") Collection<String> intervalNames,
            @Param("historyLimit") int historyLimit
    );

    interface CandleHistoryState {
        String getIntervalName();

        long getCandleCount();

        Instant getLatestCloseTime();
    }

    Optional<MarketCandle> findByPairIdAndIntervalNameAndOpenTime(
            Long pairId,
            String intervalName,
            Instant openTime
    );

    long countByPairIdAndIntervalName(Long pairId, String intervalName);

    Optional<MarketCandle> findFirstByPairIdAndIntervalNameOrderByOpenTimeDesc(Long pairId, String intervalName);

    @Query("""
            from MarketCandle candle
            join fetch candle.pair
            where candle.pair.id = :pairId
                and candle.intervalName = :intervalName
            order by candle.openTime desc
            """)
    List<MarketCandle> findByPairIdAndIntervalNameOrderByOpenTimeDesc(
            @Param("pairId") Long pairId,
            @Param("intervalName") String intervalName,
            Pageable pageable
    );
}
