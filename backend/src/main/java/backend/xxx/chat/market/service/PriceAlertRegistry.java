package backend.xxx.chat.market.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import backend.xxx.chat.market.model.PriceAlertRule;
import backend.xxx.chat.market.repository.PriceAlertRepository;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAlertRegistry {

    private final PriceAlertRepository priceAlertRepository;
    private final Map<Long, List<PriceAlertRule>> rulesByPairId = new ConcurrentHashMap<>();

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener(ApplicationReadyEvent.class)
    public void loadActiveRules() {
        rulesByPairId.clear();
        priceAlertRepository.findAllByActiveTrueWithDetails()
                .stream()
                .map(PriceAlertRule::createFrom)
                .forEach(rule -> rulesByPairId.compute(
                        rule.getPairId(),
                        (pairId, currentRules) -> append(currentRules, rule)
                ));
    }

    public List<Long> matchingAlertIds(MarketTickerLatestHash ticker) {
        if (ticker == null || ticker.getPairId() == null) {
            return List.of();
        }
        return rulesByPairId.getOrDefault(ticker.getPairId(), List.of())
                .stream()
                .filter(rule -> rule.matches(ticker))
                .map(PriceAlertRule::getAlertId)
                .toList();
    }

    public void refreshPairsAfterCommit(Collection<Long> pairIds) {
        Runnable refresh = () -> {
            try {
                pairIds.stream()
                        .filter(pairId -> pairId != null && pairId > 0)
                        .distinct()
                        .forEach(this::refreshPair);
            } catch (RuntimeException exception) {
                log.warn("Could not refresh active price alert rules", exception);
            }
        };

        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            refresh.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refresh.run();
            }
        });
    }

    private void refreshPair(Long pairId) {
        List<PriceAlertRule> rules = priceAlertRepository.findAllByPair_IdAndActiveTrueWithDetails(pairId)
                .stream()
                .map(PriceAlertRule::createFrom)
                .toList();
        if (rules.isEmpty()) {
            rulesByPairId.remove(pairId);
            return;
        }
        rulesByPairId.put(pairId, rules);
    }

    private List<PriceAlertRule> append(List<PriceAlertRule> currentRules, PriceAlertRule rule) {
        if (currentRules == null || currentRules.isEmpty()) {
            return List.of(rule);
        }
        return Stream.concat(currentRules.stream(), Stream.of(rule))
                .toList();
    }
}
