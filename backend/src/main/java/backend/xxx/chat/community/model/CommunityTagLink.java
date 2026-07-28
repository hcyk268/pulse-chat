package backend.xxx.chat.community.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "community_tag_links")
@NoArgsConstructor
@AllArgsConstructor
public class CommunityTagLink {

    @EmbeddedId
    private CommunityTagLinkId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("communityId")
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private CommunityTag tag;

    public static CommunityTagLink create(Community community, CommunityTag tag) {
        CommunityTagLink link = new CommunityTagLink();
        link.id = new CommunityTagLinkId(community.getId(), tag.getId());
        link.community = community;
        link.tag = tag;
        return link;
    }
}
