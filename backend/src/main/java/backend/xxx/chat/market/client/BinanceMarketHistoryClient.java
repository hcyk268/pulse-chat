package backend.xxx.chat.market.client;

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
                .map(BinanceKlineResponse::from)
                .toList();
    }
}