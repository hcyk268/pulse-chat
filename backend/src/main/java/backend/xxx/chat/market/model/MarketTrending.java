package backend.xxx.chat.market.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.common.model.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    private static final int COINGECKO_ID_MAX_LENGTH = 100;
    private static final int SYMBOL_MAX_LENGTH = 20;
    private static final int NAME_MAX_LENGTH = 100;
    private static final int THUMB_URL_MAX_LENGTH = 500;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private MarketAsset asset;

    @Column(name = "coingecko_id", nullable = false, length = COINGECKO_ID_MAX_LENGTH)
    private String coingeckoId;

    @Column(name = "symbol", nullable = false, length = SYMBOL_MAX_LENGTH)
    private String symbol;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "thumb_url", length = THUMB_URL_MAX_LENGTH)
    private String thumbUrl;

    @Column(name = "market_cap_rank")
    private Integer marketCapRank;

    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    public void syncSnapshot(
            MarketAsset asset,
            String coingeckoId,
            String symbol,
            String name,
            String thumbUrl,
            Integer marketCapRank,
            Integer score,
            Instant snapshotAt
    ) {
        this.asset = asset;
        this.coingeckoId = requireText(coingeckoId, "market.trending.coingecko-id", COINGECKO_ID_MAX_LENGTH);
        this.symbol = requireText(normalizeSymbol(symbol), "market.trending.symbol", SYMBOL_MAX_LENGTH);
        this.name = requireText(name, "market.trending.name", NAME_MAX_LENGTH);
        this.thumbUrl = optionalText(thumbUrl, "market.trending.thumb-url", THUMB_URL_MAX_LENGTH);
        this.marketCapRank = requirePositiveIfPresent(marketCapRank, "market.trending.market-cap-rank");
        this.score = requireNonNegative(score == null ? 0 : score, "market.trending.score");
        this.snapshotAt = Objects.requireNonNull(snapshotAt, "market.trending.snapshot-at.required");
    }

    private String normalizeSymbol(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String fieldName, int maxLength) {
        String normalizedValue = optionalText(value, fieldName, maxLength);
        if (normalizedValue == null) {
            throw new ValidationException(fieldName + ".required");
        }
        return normalizedValue;
    }

    private String optionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }
        if (trimmedValue.length() > maxLength) {
            throw new ValidationException(fieldName + ".max.length");
        }
        return trimmedValue;
    }

    private Integer requirePositiveIfPresent(Integer value, String fieldName) {
        if (value != null && value <= 0) {
            throw new ValidationException(fieldName + ".positive");
        }
        return value;
    }

    private Integer requireNonNegative(Integer value, String fieldName) {
        if (value < 0) {
            throw new ValidationException(fieldName + ".non-negative");
        }
        return value;
    }
}