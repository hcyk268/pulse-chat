package backend.xxx.chat.market.model;

import java.util.Objects;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.user.model.User;
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
@Table(
        name = "user_watchlist_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_watchlist_items_user_asset", columnNames = {"user_id", "asset_id"})
)
@NoArgsConstructor
@AllArgsConstructor
public class UserWatchlistItem extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private MarketAsset asset;

    public static UserWatchlistItem create(User user, MarketAsset asset) {
        UserWatchlistItem item = new UserWatchlistItem();
        item.user = Objects.requireNonNull(user, "watchlist.user.required");
        item.changeAsset(asset);
        return item;
    }

    public void changeAsset(MarketAsset asset) {
        this.asset = Objects.requireNonNull(asset, "watchlist.asset.required");
    }

    public boolean hasActiveAsset() {
        return asset != null && asset.isActive();
    }
}