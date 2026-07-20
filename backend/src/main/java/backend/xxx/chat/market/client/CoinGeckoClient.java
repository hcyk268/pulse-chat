package backend.xxx.chat.market.client;

import java.util.List;

import backend.xxx.chat.market.client.dto.CoinMarketResponse;
import backend.xxx.chat.market.client.dto.CoinTrendingResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CoinGeckoClient {

    private final RestClient restClient;

    public CoinGeckoClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.coingecko.com/api/v3")
                .defaultHeader("accept", "application/json")
                .build();
    }

    public List<CoinMarketResponse> getTopMarkets() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/markets")
                        .queryParam("vs_currency", "usd")
                        .queryParam("order", "market_cap_desc")
                        .queryParam("per_page", "200")
                        .queryParam("page", "1")
                        .queryParam("sparkline", "false")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public CoinTrendingResponse getTrending() {
        return restClient.get()
                .uri("/search/trending")
                .retrieve()
                .body(CoinTrendingResponse.class);
    }
}
