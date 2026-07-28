package backend.xxx.chat.community.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CommunityTagLinkId implements Serializable {

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "tag_id")
    private Long tagId;
}
