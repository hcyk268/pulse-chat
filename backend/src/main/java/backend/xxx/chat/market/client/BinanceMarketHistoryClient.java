package backend.xxx.chat.market.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.client.dto.BinanceKlineResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BinanceMarketHistoryClient {

    private final RestClient restClient;

    public BinanceMarketHistoryClient(RestClient.Builder restClientBuilder, BinanceStreamProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getRestBaseUrl())
                .defaultHeader("accept", "application/json")
                .build();
    }

    public List<BinanceKlineResponse> getKlines(String symbol, String interval, int limit) {
        List<List<Object>> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (body == null) {
            return List.of();
        }

        return body.stream()
                .filter(values -> values != null && values.size() >= 9)
                .map(this::toKlineResponse)
                .toList();
    }

    private BinanceKlineResponse toKlineResponse(List<Object> values) {
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

    private Instant instant(Object value) {
        return Instant.ofEpochMilli(number(value));
    }

    private BigDecimal decimal(Object value) {
        return new BigDecimal(String.valueOf(value));
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
