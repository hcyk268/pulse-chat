package backend.xxx.chat.ai.tool.impl;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.tool.AiTool;
import backend.xxx.chat.ai.tool.AiToolAccess;
import backend.xxx.chat.market.dto.OverviewMarketResponse;
import backend.xxx.chat.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetMarketTool implements AiTool<GetMarketTool.Input, OverviewMarketResponse> {

    private final MarketService marketService;

    @Override
    public String name() {
        return "getMarket";
    }

    @Override
    public String description() {
        return "Return market overview and trending coins.";
    }

    @Override
    public String argumentSchema() {
        return "{}";
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
    public OverviewMarketResponse execute(Input input, AiExecutionContext context) {
        return marketService.getMarket();
    }

    public record Input() {
    }
}