package backend.xxx.chat.market.redis.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@RedisHash("market_stream_state")
@NoArgsConstructor
@AllArgsConstructor
public class MarketStreamStateHash {

    @Id
    private String exchange;

    private boolean connected;
    private Instant lastHeartbeatAt;
    private Instant lastTickerEventAt;
    private Instant lastKlineEventAt;
    private Integer activeSymbolCount;
    private String lastErrorMessage;
    private Instant updatedAt;
}
