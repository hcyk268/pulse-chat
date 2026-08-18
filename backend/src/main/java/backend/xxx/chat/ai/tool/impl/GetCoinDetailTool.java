package backend.xxx.chat.ai.tool.impl;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.tool.AiTool;
import backend.xxx.chat.ai.tool.AiToolAccess;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.market.dto.CoinDetailResponse;
import backend.xxx.chat.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCoinDetailTool implements AiTool<GetCoinDetailTool.Input, CoinDetailResponse> {

    private final MarketService marketService;

    @Override
    public String name() {
        return "getCoinDetail";
    }

    @Override
    public String description() {
        return "Return coin detail by symbol.";
    }

    @Override
    public String argumentSchema() {
        return "{\"symbol\": string}";
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
    public CoinDetailResponse execute(Input input, AiExecutionContext context) {
        if (input.symbol() == null || input.symbol().isBlank()) {
            throw new ValidationException("symbol must not be blank");
        }
        return marketService.getCoinDetail(input.symbol());
    }

    public record Input(String symbol) {
    }
}