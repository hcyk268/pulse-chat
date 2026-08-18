package backend.xxx.chat.ai.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import backend.xxx.chat.ai.dto.MarketInsightRequest;
import backend.xxx.chat.ai.dto.MarketInsightResponse;
import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.orchestration.AiExecutionContextFactory;
import backend.xxx.chat.ai.orchestration.AiOrchestrator;
import backend.xxx.chat.ai.orchestration.AiUseCaseType;
import backend.xxx.chat.ai.prompt.InstructionPrompt;
import backend.xxx.chat.ai.safety.AiResponseValidator;
import backend.xxx.chat.ai.safety.SensitiveDataRedactor;
import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.market.dto.CoinDetailResponse;
import backend.xxx.chat.market.dto.MarketCandleResponse;
import backend.xxx.chat.market.dto.OverviewMarketResponse;
import backend.xxx.chat.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MarketInsightUseCase {

    private static final String DAILY_CANDLE_INTERVAL = "1d";
    private static final int RECENT_DAILY_CANDLE_LIMIT = 7;

    private final AiExecutionContextFactory contextFactory;
    private final AiOrchestrator orchestrator;
    private final AiResponseValidator responseValidator;
    private final SensitiveDataRedactor redactor;
    private final MarketService marketService;

    public MarketInsightResponse generate(MarketInsightRequest request) {
        AiExecutionContext context = contextFactory.create(AiUseCaseType.MARKET_INSIGHT);
        String symbol = normalizeSymbol(request == null ? null : request.symbol());
        return generateInsight(context, symbol);
    }

    private MarketInsightResponse generateInsight(AiExecutionContext context, String symbol) {
        OverviewMarketResponse market = marketService.getMarket();
        CoinDetailInput coinDetail = loadCoinDetail(symbol);
        CandleInput recentDailyCandles = loadRecentDailyCandles(symbol);
        MarketInsightInput input = new MarketInsightInput(symbol, market, coinDetail, recentDailyCandles);
        var structured = orchestrator.completeStructuredTask(
                context,
                InstructionPrompt.MARKET_INSIGHT_PROMPT,
                input,
                MarketInsightOutput.class
        );
        MarketInsightOutput output = structured.entity();
        return new MarketInsightResponse(
                StringUtils.hasText(symbol) ? symbol : "OVERVIEW",
                redactor.redact(responseValidator.requiredPlainText(output.insight(), 2_000)),
                redactor.redactAll(boundedList(output.keyPoints(), 8, 240)),
                redactor.redactAll(boundedList(output.riskNotes(), 6, 240)),
                Instant.now(),
                structured.model()
        );
    }

    private List<String> boundedList(List<String> values, int maxItems, int maxItemLength) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .limit(maxItems)
                .map(value -> value.length() <= maxItemLength ? value.trim() : value.substring(0, maxItemLength).trim())
                .toList();
    }
    private CoinDetailInput loadCoinDetail(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return new CoinDetailInput(false, false, null, null, null);
        }
        try {
            return new CoinDetailInput(true, true, marketService.getCoinDetail(symbol), null, null);
        } catch (ApiException ex) {
            if (isPermissionFailure(ex)) {
                throw ex;
            }
            return new CoinDetailInput(
                    true,
                    false,
                    null,
                    ex.getCode() == null ? "API_ERROR" : ex.getCode().name(),
                    StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "market.coin.detail.unavailable"
            );
        }
    }

    private CandleInput loadRecentDailyCandles(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return new CandleInput(false, false, DAILY_CANDLE_INTERVAL, 0, List.of(), null, null);
        }
        try {
            List<MarketCandleResponse> candles = marketService.getCandles(symbol, DAILY_CANDLE_INTERVAL);
            List<MarketCandleResponse> limitedCandles = candles.stream()
                    .skip(Math.max(0, candles.size() - RECENT_DAILY_CANDLE_LIMIT))
                    .toList();
            return new CandleInput(
                    true,
                    true,
                    DAILY_CANDLE_INTERVAL,
                    limitedCandles.size(),
                    limitedCandles,
                    null,
                    null
            );
        } catch (ApiException ex) {
            if (isPermissionFailure(ex)) {
                throw ex;
            }
            return new CandleInput(
                    true,
                    false,
                    DAILY_CANDLE_INTERVAL,
                    0,
                    List.of(),
                    ex.getCode() == null ? "API_ERROR" : ex.getCode().name(),
                    StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "market.candles.unavailable"
            );
        } catch (IllegalArgumentException ex) {
            return new CandleInput(
                    true,
                    false,
                    DAILY_CANDLE_INTERVAL,
                    0,
                    List.of(),
                    "VALIDATION_ERROR",
                    StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "market.candles.unavailable"
            );
        }
    }

    private boolean isPermissionFailure(ApiException ex) {
        int status = ex.getStatus() == null ? 500 : ex.getStatus().value();
        return status == 401 || status == 403;
    }

    private String normalizeSymbol(String symbol) {
        return StringUtils.hasText(symbol) ? symbol.trim().toUpperCase(Locale.ROOT) : null;
    }

    private record MarketInsightInput(
            String symbol,
            OverviewMarketResponse market,
            CoinDetailInput coinDetail,
            CandleInput recentDailyCandles
    ) {
    }

    private record CoinDetailInput(
            boolean requested,
            boolean available,
            CoinDetailResponse data,
            String errorCode,
            String message
    ) {
    }

    private record CandleInput(
            boolean requested,
            boolean available,
            String interval,
            int count,
            List<MarketCandleResponse> candles,
            String errorCode,
            String message
    ) {
    }
}