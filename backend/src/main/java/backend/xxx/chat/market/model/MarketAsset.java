package backend.xxx.chat.market.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.common.model.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    private static final int COINGECKO_ID_MAX_LENGTH = 100;
    private static final int SYMBOL_MAX_LENGTH = 20;
    private static final int NAME_MAX_LENGTH = 100;
    private static final int IMAGE_URL_MAX_LENGTH = 500;

    @Column(name = "coingecko_id", nullable = false, length = COINGECKO_ID_MAX_LENGTH)
    private String coingeckoId;

    @Column(name = "symbol", nullable = false, length = SYMBOL_MAX_LENGTH)
    private String symbol;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "image_url", length = IMAGE_URL_MAX_LENGTH)
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

    public void syncMarketData(
            String coingeckoId,
            String symbol,
            String name,
            String imageUrl,
            Integer marketCapRank,
            BigDecimal currentPriceUsd,
            BigDecimal priceChangePercentage24h,
            BigDecimal high24h,
            BigDecimal low24h,
            BigDecimal marketCap,
            BigDecimal totalVolume,
            BigDecimal circulatingSupply,
            BigDecimal totalSupply,
            BigDecimal maxSupply,
            boolean active,
            Instant syncedAt
    ) {
        this.coingeckoId = requireText(coingeckoId, "market.asset.coingecko-id", COINGECKO_ID_MAX_LENGTH);
        this.symbol = requireText(normalizeSymbol(symbol), "market.asset.symbol", SYMBOL_MAX_LENGTH);
        this.name = requireText(name, "market.asset.name", NAME_MAX_LENGTH);
        this.imageUrl = optionalText(imageUrl, "market.asset.image-url", IMAGE_URL_MAX_LENGTH);
        this.marketCapRank = requirePositiveIfPresent(marketCapRank, "market.asset.market-cap-rank");
        this.currentPriceUsd = requireNonNegativeIfPresent(currentPriceUsd, "market.asset.current-price-usd");
        this.priceChangePercentage24h = priceChangePercentage24h;
        this.high24h = requireNonNegativeIfPresent(high24h, "market.asset.high-24h");
        this.low24h = requireNonNegativeIfPresent(low24h, "market.asset.low-24h");
        this.marketCap = requireNonNegativeIfPresent(marketCap, "market.asset.market-cap");
        this.totalVolume = requireNonNegativeIfPresent(totalVolume, "market.asset.total-volume");
        this.circulatingSupply = requireNonNegativeIfPresent(circulatingSupply, "market.asset.circulating-supply");
        this.totalSupply = requireNonNegativeIfPresent(totalSupply, "market.asset.total-supply");
        this.maxSupply = requireNonNegativeIfPresent(maxSupply, "market.asset.max-supply");
        this.active = active;
        this.lastSyncedAt = syncedAt;
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

    private BigDecimal requireNonNegativeIfPresent(BigDecimal value, String fieldName) {
        if (value != null && value.signum() < 0) {
            throw new ValidationException(fieldName + ".non-negative");
        }
        return value;
    }
}