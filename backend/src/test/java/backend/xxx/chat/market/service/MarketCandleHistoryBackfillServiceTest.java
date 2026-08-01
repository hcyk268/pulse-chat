package backend.xxx.chat.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.client.BinanceMarketHistoryClient;
import backend.xxx.chat.market.client.dto.BinanceKlineResponse;
import backend.xxx.chat.market.model.MarketCandle;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.repository.MarketCandleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketCandleHistoryBackfillServiceTest {

    @Mock
    private BinanceMarketHistoryClient binanceMarketHistoryClient;

    @Mock
    private MarketCandleRepository marketCandleRepository;

    @Mock
    private BinanceStreamProperties properties;

    @InjectMocks
    private MarketCandleHistoryBackfillService service;

    @Test
    @SuppressWarnings("unchecked")
    void batchesStateAndExistingCandleQueriesAcrossIntervals() {
        MarketPair pair = new MarketPair();
        pair.setId(10L);
        pair.setSymbol("BTCUSDT");

        Instant now = Instant.now();
        BinanceKlineResponse fourHour = kline(now.minusSeconds(14_400), now.minusSeconds(1));
        BinanceKlineResponse oneDay = kline(now.minusSeconds(86_400), now.minusSeconds(2));

        when(properties.getCandleHistoryLimit()).thenReturn(80);
        when(marketCandleRepository.findHistoryStates(10L, List.of("4h", "1d")))
                .thenReturn(List.of());
        when(binanceMarketHistoryClient.getKlines("BTCUSDT", "4h", 81))
                .thenReturn(List.of(fourHour));
        when(binanceMarketHistoryClient.getKlines("BTCUSDT", "1d", 81))
                .thenReturn(List.of(oneDay));
        when(marketCandleRepository.findExistingCandles(
                eq(10L),
                anyCollection(),
                anyCollection()
        )).thenReturn(List.of());
        when(marketCandleRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.backfillIfNeeded(pair, List.of("4h", "1d"));

        verify(marketCandleRepository).findHistoryStates(10L, List.of("4h", "1d"));
        verify(marketCandleRepository).findExistingCandles(
                eq(10L),
                anyCollection(),
                anyCollection()
        );
        ArgumentCaptor<List<MarketCandle>> candlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(marketCandleRepository).saveAll(candlesCaptor.capture());
        assertThat(candlesCaptor.getValue())
                .extracting(MarketCandle::getIntervalName)
                .containsExactly("4h", "1d");
    }

    private BinanceKlineResponse kline(Instant openTime, Instant closeTime) {
        return new BinanceKlineResponse(
                openTime,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(105),
                BigDecimal.TEN,
                closeTime,
                BigDecimal.valueOf(1000),
                20L
        );
    }
}
