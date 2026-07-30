package backend.xxx.chat.market.client;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.client.dto.BinanceExchangeInfoResponse;
import backend.xxx.chat.market.client.dto.BinanceSymbolResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BinanceExchangeInfoClient {

    private static final String TRADING_STATUS = "TRADING";

    private final RestClient restClient;

    public BinanceExchangeInfoClient(RestClient.Builder restClientBuilder, BinanceStreamProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getRestBaseUrl())
                .defaultHeader("accept", "application/json")
                .build();
    }

    public Set<String> getTradingSpotSymbolsByQuote(String quoteSymbol) {
        BinanceExchangeInfoResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/exchangeInfo")
                        .queryParam("permissions", "SPOT")
                        .build())
                .retrieve()
                .body(BinanceExchangeInfoResponse.class);

        List<BinanceSymbolResponse> symbols = response == null
                ? List.of()
                : response.symbols();
        if (symbols == null || symbols.isEmpty()) {
            return Set.of();
        }

        String normalizedQuoteSymbol = quoteSymbol.toUpperCase(Locale.ROOT);
        return symbols.stream()
                .filter(symbol -> symbol != null && symbol.symbol() != null)
                .filter(symbol -> normalizedQuoteSymbol.equalsIgnoreCase(symbol.quoteAsset()))
                .filter(symbol -> TRADING_STATUS.equalsIgnoreCase(symbol.status()))
                .filter(symbol -> Boolean.TRUE.equals(symbol.isSpotTradingAllowed()))
                .map(BinanceSymbolResponse::symbol)
                .map(symbol -> symbol.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
