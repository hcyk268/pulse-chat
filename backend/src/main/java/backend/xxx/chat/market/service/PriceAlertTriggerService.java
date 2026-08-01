package backend.xxx.chat.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import backend.xxx.chat.market.model.PriceAlert;
import backend.xxx.chat.market.model.PriceAlertRule;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import backend.xxx.chat.market.repository.PriceAlertRepository;
import backend.xxx.chat.notification.model.NotificationTargetType;
import backend.xxx.chat.notification.model.NotificationType;
import backend.xxx.chat.notification.service.NotificationCommand;
import backend.xxx.chat.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceAlertTriggerService {

    private final PriceAlertRepository priceAlertRepository;
    private final PriceAlertRegistry priceAlertRegistry;
    private final NotificationService notificationService;

    @Transactional
    public void triggerIfMatched(Long alertId, MarketTickerLatestHash ticker) {
        triggerMatched(List.of(alertId), ticker);
    }

    @Transactional
    public void triggerMatched(Collection<Long> alertIds, MarketTickerLatestHash ticker) {
        if (alertIds == null || alertIds.isEmpty()) {
            return;
        }

        List<Long> normalizedAlertIds = alertIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalizedAlertIds.isEmpty()) {
            return;
        }

        List<PriceAlert> alerts =
                priceAlertRepository.findAllByIdInAndActiveTrueWithDetails(normalizedAlertIds);
        if (alerts.isEmpty()) {
            return;
        }

        Instant triggeredAt = ticker.getEventTime() == null ? Instant.now() : ticker.getEventTime();
        List<NotificationCommand> notifications = new ArrayList<>();
        Set<Long> affectedPairIds = new LinkedHashSet<>();
        for (PriceAlert alert : alerts) {
            if (!PriceAlertRule.createFrom(alert).matches(ticker)
                    || !alert.markTriggered(ticker.getPrice(), triggeredAt)) {
                continue;
            }
            notifications.add(toNotificationCommand(alert, ticker, triggeredAt));
            affectedPairIds.add(alert.getPair().getId());
        }

        notificationService.createAll(notifications);
        priceAlertRegistry.refreshPairsAfterCommit(affectedPairIds);
    }

    private NotificationCommand toNotificationCommand(
            PriceAlert alert,
            MarketTickerLatestHash ticker,
            Instant triggeredAt
    ) {
        return new NotificationCommand(
                alert.getUser(),
                null,
                NotificationType.PRICE_ALERT,
                "Price alert triggered",
                buildBody(alert, ticker),
                NotificationTargetType.PRICE_ALERT,
                alert.getId(),
                "PRICE_ALERT",
                alert.getId(),
                "price-alert:" + alert.getId() + ":" + triggeredAt.toEpochMilli()
        );
    }

    private String buildBody(PriceAlert alert, MarketTickerLatestHash ticker) {
        String symbol = alert.getAsset().getSymbol();
        String price = decimalText(ticker.getPrice());
        return switch (alert.getConditionType()) {
            case ABOVE -> symbol + " reached or exceeded " + decimalText(alert.getTargetPrice())
                    + ". Current price: " + price;
            case BELOW -> symbol + " reached or fell below " + decimalText(alert.getTargetPrice())
                    + ". Current price: " + price;
            case CHANGE_PERCENT -> symbol + " changed "
                    + decimalText(ticker.getPriceChangePercent())
                    + "% in 24h. Alert threshold: "
                    + decimalText(alert.getTargetPercent())
                    + "%";
        };
    }

    private String decimalText(BigDecimal value) {
        return value == null ? "n/a" : value.stripTrailingZeros().toPlainString();
    }
}
