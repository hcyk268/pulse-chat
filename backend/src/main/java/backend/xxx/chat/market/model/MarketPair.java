package backend.xxx.chat.market.model;

import java.time.Instant;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "market_pairs")
@NoArgsConstructor
@AllArgsConstructor
public class MarketPair extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private MarketAsset asset;

    @Column(name = "exchange", nullable = false, length = 30)
    private String exchange;

    @Column(name = "base_symbol", nullable = false, length = 20)
    private String baseSymbol;

    @Column(name = "quote_symbol", nullable = false, length = 20)
    private String quoteSymbol;

    @Column(name = "symbol", nullable = false, length = 40)
    private String symbol;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
