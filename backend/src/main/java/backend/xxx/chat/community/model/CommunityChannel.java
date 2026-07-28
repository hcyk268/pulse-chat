package backend.xxx.chat.community.model;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.conversation.model.Conversation;
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
        name = "community_channels",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_community_channels_community_slug", columnNames = {"community_id", "slug"}),
                @UniqueConstraint(name = "uk_community_channels_conversation", columnNames = "conversation_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class CommunityChannel extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CommunityChannelType type = CommunityChannelType.TEXT;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_default", nullable = false)
    private boolean defaultChannel;

    @Column(name = "is_read_only", nullable = false)
    private boolean readOnly;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommunityChannelStatus status = CommunityChannelStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    public static CommunityChannel create(
            Community community,
            Conversation conversation,
            String slug,
            String name,
            String description,
            CommunityChannelType type,
            int sortOrder,
            boolean defaultChannel,
            boolean readOnly,
            User createdBy
    ) {
        CommunityChannel channel = new CommunityChannel();
        channel.community = community;
        channel.conversation = conversation;
        channel.slug = slug;
        channel.name = name;
        channel.description = description;
        channel.type = type == null ? CommunityChannelType.TEXT : type;
        channel.sortOrder = sortOrder;
        channel.defaultChannel = defaultChannel;
        channel.readOnly = readOnly;
        channel.status = CommunityChannelStatus.ACTIVE;
        channel.createdBy = createdBy;
        return channel;
    }

    public boolean isActive() {
        return status == CommunityChannelStatus.ACTIVE;
    }
}
