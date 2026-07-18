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
@Table(name = "market_trending")
@NoArgsConstructor
@AllArgsConstructor
public class MarketTrending extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private MarketAsset asset;

    @Column(name = "coingecko_id", nullable = false, length = 100)
    private String coingeckoId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "thumb_url", length = 500)
    private String thumbUrl;

    @Column(name = "market_cap_rank")
    private Integer marketCapRank;

    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;
}
