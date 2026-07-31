package backend.xxx.chat.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
        PriceAlert alert = priceAlertRepository.findByIdAndActiveTrueWithDetails(alertId).orElse(null);
        if (alert == null || !PriceAlertRule.createFrom(alert).matches(ticker)) {
            return;
        }

        Instant triggeredAt = ticker.getEventTime() == null ? Instant.now() : ticker.getEventTime();
        if (!alert.markTriggered(ticker.getPrice(), triggeredAt)) {
            return;
        }

        notificationService.create(new NotificationCommand(
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
        ));
        priceAlertRegistry.refreshPairsAfterCommit(List.of(alert.getPair().getId()));
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
