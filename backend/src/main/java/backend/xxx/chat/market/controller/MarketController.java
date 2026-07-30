package backend.xxx.chat.market.controller;

import java.util.List;

import backend.xxx.chat.common.dto.ResponseData;
import backend.xxx.chat.market.dto.CoinDetailResponse;
import backend.xxx.chat.market.dto.MarketCandleResponse;
import backend.xxx.chat.market.dto.MarketTickerResponse;
import backend.xxx.chat.market.dto.OverviewMarketResponse;
import backend.xxx.chat.market.service.MarketService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Validated
public class MarketController {

    private final MarketService marketService;

    @GetMapping
    public ResponseData<OverviewMarketResponse> getMarket() {
        return new ResponseData<>(true, "", marketService.getMarket());
    }

    @GetMapping("/tickers")
    public ResponseData<List<MarketTickerResponse>> getTickers() {
        return new ResponseData<>(true, "", marketService.getTickers());
    }

    @GetMapping("/tickers/{symbol}")
    public ResponseData<MarketTickerResponse> getTicker(@NotBlank @PathVariable String symbol) {
        return new ResponseData<>(true, "", marketService.getTicker(symbol));
    }

    @GetMapping("/tickers/{symbol}/candles")
    public ResponseData<List<MarketCandleResponse>> getTickerCandles(
            @NotBlank @PathVariable String symbol,
            @RequestParam(defaultValue = "4h") String interval
    ) {
        return new ResponseData<>(true, "", marketService.getCandles(symbol, interval));
    }

    @GetMapping("/{symbol}")
    public ResponseData<CoinDetailResponse> getCoinDetail(@NotBlank @PathVariable String symbol) {
        return new ResponseData<>(true, "", marketService.getCoinDetail(symbol));
    }
}

