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
@Table(name = "market_pairs")
@NoArgsConstructor
@AllArgsConstructor
public class MarketPair extends AbstractBaseEntity<Long> {

    private static final int EXCHANGE_MAX_LENGTH = 30;
    private static final int ASSET_SYMBOL_MAX_LENGTH = 20;
    private static final int PAIR_SYMBOL_MAX_LENGTH = 40;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private MarketAsset asset;

    @Column(name = "exchange", nullable = false, length = EXCHANGE_MAX_LENGTH)
    private String exchange;

    @Column(name = "base_symbol", nullable = false, length = ASSET_SYMBOL_MAX_LENGTH)
    private String baseSymbol;

    @Column(name = "quote_symbol", nullable = false, length = ASSET_SYMBOL_MAX_LENGTH)
    private String quoteSymbol;

    @Column(name = "symbol", nullable = false, length = PAIR_SYMBOL_MAX_LENGTH)
    private String symbol;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    public void syncPair(
            MarketAsset asset,
            String exchange,
            String baseSymbol,
            String quoteSymbol,
            String symbol,
            boolean active,
            Instant syncedAt
    ) {
        this.asset = Objects.requireNonNull(asset, "market-pair.asset.required");
        this.exchange = requireText(normalize(exchange), "market.pair.exchange", EXCHANGE_MAX_LENGTH);
        this.baseSymbol = requireText(normalize(baseSymbol), "market.pair.base-symbol", ASSET_SYMBOL_MAX_LENGTH);
        this.quoteSymbol = requireText(normalize(quoteSymbol), "market.pair.quote-symbol", ASSET_SYMBOL_MAX_LENGTH);
        this.symbol = requireText(normalize(symbol), "market.pair.symbol", PAIR_SYMBOL_MAX_LENGTH);
        this.active = active;
        this.lastSyncedAt = syncedAt;
    }

    public void deactivate(Instant syncedAt) {
        this.active = false;
        this.lastSyncedAt = syncedAt;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + ".required");
        }
        if (value.length() > maxLength) {
            throw new ValidationException(fieldName + ".max.length");
        }
        return value;
    }
}