package backend.xxx.chat.ai.tool.impl;

import java.util.List;
import java.util.Locale;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.tool.AiTool;
import backend.xxx.chat.ai.tool.AiToolAccess;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.market.dto.MarketCandleResponse;
import backend.xxx.chat.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class GetTickerCandlesTool implements AiTool<GetTickerCandlesTool.Input, GetTickerCandlesTool.Output> {

    private static final String DEFAULT_INTERVAL = "1d";
    private static final int DEFAULT_LIMIT = 7;
    private static final int MAX_LIMIT = 30;

    private final MarketService marketService;

    @Override
    public String name() {
        return "getTickerCandles";
    }

    @Override
    public String description() {
        return "Return recent OHLCV candles for a market symbol. Use this for recent-days price movement.";
    }

    @Override
    public String argumentSchema() {
        return "{\"symbol\": string, \"interval\": string optional default 1d, \"limit\": number optional 1..30}";
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public AiToolAccess access() {
        return AiToolAccess.READ_ONLY;
    }

    @Override
    public Output execute(Input input, AiExecutionContext context) {
        if (!StringUtils.hasText(input.symbol())) {
            throw new ValidationException("symbol must not be blank");
        }
        String interval = StringUtils.hasText(input.interval()) ? input.interval().trim() : DEFAULT_INTERVAL;
        int limit = normalizeLimit(input.limit());
        List<MarketCandleResponse> candles = marketService.getCandles(input.symbol(), interval);
        List<MarketCandleResponse> limitedCandles = candles.stream()
                .skip(Math.max(0, candles.size() - limit))
                .toList();
        return new Output(input.symbol().trim().toUpperCase(Locale.ROOT), interval, limitedCandles.size(), limitedCandles);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    public record Input(String symbol, String interval, Integer limit) {
    }

    public record Output(
            String symbol,
            String interval,
            int count,
            List<MarketCandleResponse> candles
    ) {
    }
}