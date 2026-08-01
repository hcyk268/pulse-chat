package backend.xxx.chat.market.service;

import backend.xxx.chat.realtime.event.TickerUpdatedDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAlertTickerEventListener {

    private final PriceAlertRegistry priceAlertRegistry;
    private final PriceAlertTriggerService priceAlertTriggerService;

    @EventListener
    public void onTickerUpdated(TickerUpdatedDomainEvent event) {
        var alertIds = priceAlertRegistry.matchingAlertIds(event.ticker());
        if (alertIds.isEmpty()) {
            return;
        }

        try {
            priceAlertTriggerService.triggerMatched(alertIds, event.ticker());
        } catch (RuntimeException exception) {
            log.warn("Could not process price alerts {}", alertIds, exception);
        }
    }
}
