package backend.xxx.chat.market.model;

import java.math.BigDecimal;
import java.time.Instant;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "market_assets")
@NoArgsConstructor
@AllArgsConstructor
public class MarketAsset extends AbstractBaseEntity<Long> {

    @Column(name = "coingecko_id", nullable = false, length = 100)
    private String coingeckoId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "market_cap_rank")
    private Integer marketCapRank;

    @Column(name = "current_price_usd", precision = 30, scale = 12)
    private BigDecimal currentPriceUsd;

    @Column(name = "price_change_percentage_24h", precision = 20, scale = 8)
    private BigDecimal priceChangePercentage24h;

    @Column(name = "high_24h", precision = 30, scale = 12)
    private BigDecimal high24h;

    @Column(name = "low_24h", precision = 30, scale = 12)
    private BigDecimal low24h;

    @Column(name = "market_cap", precision = 30, scale = 2)
    private BigDecimal marketCap;

    @Column(name = "total_volume", precision = 30, scale = 2)
    private BigDecimal totalVolume;

    @Column(name = "circulating_supply", precision = 30, scale = 12)
    private BigDecimal circulatingSupply;

    @Column(name = "total_supply", precision = 30, scale = 12)
    private BigDecimal totalSupply;

    @Column(name = "max_supply", precision = 30, scale = 12)
    private BigDecimal maxSupply;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
