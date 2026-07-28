package backend.xxx.chat.community.model;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.storage.model.UploadedAsset;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "communities",
        uniqueConstraints = @UniqueConstraint(name = "uk_communities_slug", columnNames = "slug")
)
@NoArgsConstructor
@AllArgsConstructor
public class Community extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CommunityCategory category;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_asset_id")
    private UploadedAsset avatarAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_asset_id")
    private UploadedAsset coverAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private CommunityVisibility visibility = CommunityVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommunityStatus status = CommunityStatus.ACTIVE;

    @Column(name = "member_count", nullable = false)
    private long memberCount;

    @Column(name = "online_count", nullable = false)
    private long onlineCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_channel_id")
    private CommunityChannel defaultChannel;

    public static Community create(
            User owner,
            CommunityCategory category,
            String slug,
            String name,
            String description,
            UploadedAsset avatarAsset,
            UploadedAsset coverAsset,
            CommunityVisibility visibility
    ) {
        Community community = new Community();
        community.owner = owner;
        community.category = category;
        community.slug = slug;
        community.name = name;
        community.description = description;
        community.avatarAsset = avatarAsset;
        community.coverAsset = coverAsset;
        community.visibility = visibility == null ? CommunityVisibility.PUBLIC : visibility;
        community.status = CommunityStatus.ACTIVE;
        community.memberCount = 0L;
        community.onlineCount = 0L;
        return community;
    }

    public void updateProfile(
            CommunityCategory category,
            String name,
            String description,
            UploadedAsset avatarAsset,
            UploadedAsset coverAsset,
            CommunityVisibility visibility
    ) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.avatarAsset = avatarAsset;
        this.coverAsset = coverAsset;
        this.visibility = visibility;
    }

    public void incrementMemberCount() {
        this.memberCount++;
    }

    public void decrementMemberCount() {
        this.memberCount = Math.max(0, this.memberCount - 1);
    }

    public boolean isActive() {
        return status == CommunityStatus.ACTIVE;
    }
}
