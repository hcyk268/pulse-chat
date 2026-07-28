package backend.xxx.chat.market.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BinanceKlineResponse(
        Instant openTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        Instant closeTime,
        BigDecimal quoteVolume,
        long tradeCount
) {

    public static BinanceKlineResponse from(List<Object> values) {
        return new BinanceKlineResponse(
                instant(values.get(0)),
                decimal(values.get(1)),
                decimal(values.get(2)),
                decimal(values.get(3)),
                decimal(values.get(4)),
                decimal(values.get(5)),
                instant(values.get(6)),
                decimal(values.get(7)),
                number(values.get(8))
        );
    }

    public boolean isClosedAt(Instant now) {
        return closeTime != null && closeTime.isBefore(now);
    }

    private static Instant instant(Object value) {
        return Instant.ofEpochMilli(number(value));
    }

    private static BigDecimal decimal(Object value) {
        return new BigDecimal(String.valueOf(value));
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}