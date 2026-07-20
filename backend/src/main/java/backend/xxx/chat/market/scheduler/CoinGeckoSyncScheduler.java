package backend.xxx.chat.market.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.market.sync",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CoinGeckoSyncScheduler {

    private final CoinGeckoSyncService coinGeckoSyncService;
    private final AtomicBoolean marketsSyncing = new AtomicBoolean(false);
    private final AtomicBoolean trendingSyncing = new AtomicBoolean(false);

    @Scheduled(
            fixedDelayString = "${app.market.sync.markets-delay-ms:1800000}",
            initialDelayString = "${app.market.sync.initial-delay-ms:10000}",
            scheduler = "marketSyncTaskScheduler"
    )
    public void syncTopMarkets() {
        if (!marketsSyncing.compareAndSet(false, true)) {
            log.debug("Previous sync is still running");
            return;
        }

        try {
            int syncedCount = coinGeckoSyncService.syncTopMarkets();
            log.info("Synced {} market asset from CoinGecko /coins/markets", syncedCount);
        } catch (RuntimeException exception) {
            log.warn("Failed to sync CoinGecko /coins/markets", exception);
        } finally {
            marketsSyncing.set(false);
        }
    }

    @Scheduled(
            fixedDelayString = "${app.market.sync.trending-delay-ms:3600000}",
            initialDelayString = "${app.market.sync.initial-delay-ms:10000}",
            scheduler = "marketSyncTaskScheduler"
    )
    public void syncTrending() {
        if (!trendingSyncing.compareAndSet(false, true)) {
            log.debug("Previous sync is still running");
            return;
        }

        try {
            int syncedCount = coinGeckoSyncService.syncTrending();
            log.info("Synced {} trending coin from CoinGecko /search/trending", syncedCount);
        } catch (RuntimeException exception) {
            log.warn("Failed to sync CoinGecko /search/trending", exception);
        } finally {
            trendingSyncing.set(false);
        }
    }
}