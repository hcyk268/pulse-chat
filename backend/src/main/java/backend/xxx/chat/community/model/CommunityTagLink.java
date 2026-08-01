package backend.xxx.chat.community.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Entity
@Table(name = "community_tag_links")
@NoArgsConstructor
@AllArgsConstructor
public class CommunityTagLink implements Persistable<CommunityTagLinkId> {

    @EmbeddedId
    private CommunityTagLinkId id;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("communityId")
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private CommunityTag tag;

    @Override
    public CommunityTagLinkId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
    }

    public static CommunityTagLink create(Community community, CommunityTag tag) {
        CommunityTagLink link = new CommunityTagLink();
        link.id = new CommunityTagLinkId(community.getId(), tag.getId());
        link.community = community;
        link.tag = tag;
        return link;
    }
}
