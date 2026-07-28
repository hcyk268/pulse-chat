package backend.xxx.chat.community.model;

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
@Table(
        name = "community_tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_community_tags_slug", columnNames = "slug")
)
@NoArgsConstructor
@AllArgsConstructor
public class CommunityTag extends AbstractBaseEntity<Long> {

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
