package backend.xxx.chat.community.model;

import java.time.Instant;

import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "community_members")
@NoArgsConstructor
@AllArgsConstructor
public class CommunityMember implements Persistable<CommunityMemberId> {

    @EmbeddedId
    private CommunityMemberId id;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("communityId")
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private CommunityMemberRole role = CommunityMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommunityMemberStatus status = CommunityMemberStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Override
    public CommunityMemberId getId() {
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

    public static CommunityMember owner(Community community, User user, Instant joinedAt) {
        CommunityMember member = active(community, user, joinedAt);
        member.role = CommunityMemberRole.OWNER;
        return member;
    }

    public static CommunityMember active(Community community, User user, Instant joinedAt) {
        CommunityMember member = new CommunityMember();
        member.id = new CommunityMemberId(community.getId(), user.getId());
        member.community = community;
        member.user = user;
        member.role = CommunityMemberRole.MEMBER;
        member.status = CommunityMemberStatus.ACTIVE;
        member.joinedAt = joinedAt;
        member.lastSeenAt = joinedAt;
        return member;
    }

    public boolean isActive() {
        return status == CommunityMemberStatus.ACTIVE;
    }

    public boolean isOwner() {
        return role == CommunityMemberRole.OWNER;
    }

    public boolean canManageCommunity() {
        return role == CommunityMemberRole.OWNER || role == CommunityMemberRole.ADMIN;
    }

    public boolean canManageChannels() {
        return role == CommunityMemberRole.OWNER
                || role == CommunityMemberRole.ADMIN
                || role == CommunityMemberRole.MODERATOR;
    }

    public void reactivate(Instant joinedAt) {
        this.status = CommunityMemberStatus.ACTIVE;
        this.joinedAt = joinedAt;
        this.lastSeenAt = joinedAt;
        if (this.role == null) {
            this.role = CommunityMemberRole.MEMBER;
        }
    }

    public void markLeft(Instant leftAt) {
        this.status = CommunityMemberStatus.LEFT;
        this.lastSeenAt = leftAt;
    }
}
