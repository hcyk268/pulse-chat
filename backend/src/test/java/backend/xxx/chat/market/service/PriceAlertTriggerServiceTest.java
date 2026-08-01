package backend.xxx.chat.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.model.PriceAlert;
import backend.xxx.chat.market.model.PriceAlertConditionType;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import backend.xxx.chat.market.repository.PriceAlertRepository;
import backend.xxx.chat.notification.service.NotificationService;
import backend.xxx.chat.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceAlertTriggerServiceTest {

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @Mock
    private PriceAlertRegistry priceAlertRegistry;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PriceAlertTriggerService service;

    @Test
    void fetchesAndNotifiesMatchingAlerts() {
        MarketAsset asset = new MarketAsset();
        asset.setId(1L);
        asset.setSymbol("BTC");
        MarketPair pair = new MarketPair();
        pair.setId(10L);
        pair.setAsset(asset);
        pair.setSymbol("BTCUSDT");

        PriceAlert first = alert(100L, user(1L, "alice"), asset, pair);
        PriceAlert second = alert(200L, user(2L, "bob"), asset, pair);
        MarketTickerLatestHash ticker = new MarketTickerLatestHash();
        ticker.setPairId(10L);
        ticker.setPrice(BigDecimal.valueOf(110));
        ticker.setEventTime(Instant.now());

        when(priceAlertRepository.findAllByIdInAndActiveTrueWithDetails(List.of(100L, 200L)))
                .thenReturn(List.of(first, second));

        service.triggerMatched(List.of(100L, 200L), ticker);

        verify(priceAlertRepository).findAllByIdInAndActiveTrueWithDetails(List.of(100L, 200L));
        verify(notificationService).createAll(argThat(commands -> commands.size() == 2));
        verify(priceAlertRegistry).refreshPairsAfterCommit(Set.of(10L));
    }

    private PriceAlert alert(
            Long id,
            User user,
            MarketAsset asset,
            MarketPair pair
    ) {
        PriceAlert alert = PriceAlert.create(
                user,
                asset,
                pair,
                PriceAlertConditionType.ABOVE,
                BigDecimal.valueOf(100),
                null,
                true
        );
        alert.setId(id);
        return alert;
    }

    private User user(Long id, String username) {
        User user = User.create(
                username,
                username + "@example.com",
                "Password123!",
                username
        );
        user.setId(id);
        return user;
    }
}
