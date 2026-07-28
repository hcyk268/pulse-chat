package backend.xxx.chat.community.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.community.model.CommunityMember;
import backend.xxx.chat.community.model.CommunityMemberId;
import backend.xxx.chat.community.model.CommunityMemberStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityMemberRepository extends JpaRepository<CommunityMember, CommunityMemberId> {

    boolean existsByCommunityIdAndUserIdAndStatus(Long communityId, Long userId, CommunityMemberStatus status);

    long countByCommunityIdAndStatus(Long communityId, CommunityMemberStatus status);

    @Query("""
            from CommunityMember member
            join fetch member.user
            where member.community.id = :communityId
                and member.status = :status
            order by member.role asc, member.joinedAt asc
            """)
    List<CommunityMember> findByCommunityIdAndStatusWithUser(
            @Param("communityId") Long communityId,
            @Param("status") CommunityMemberStatus status
    );

    @Query("""
            from CommunityMember member
            join fetch member.user
            where member.community.id = :communityId
                and member.status = :status
            order by member.role asc, member.joinedAt asc
            """)
    List<CommunityMember> findPageByCommunityIdAndStatusWithUser(
            @Param("communityId") Long communityId,
            @Param("status") CommunityMemberStatus status,
            Pageable pageable
    );

    @Query("""
            from CommunityMember member
            join fetch member.user
            where member.community.id = :communityId
                and member.user.id in :userIds
            """)
    List<CommunityMember> findByCommunityIdAndUserIdInWithUser(
            @Param("communityId") Long communityId,
            @Param("userIds") Collection<Long> userIds
    );

    @Query("""
            from CommunityMember member
            join fetch member.community
            where member.community.id = :communityId
                and member.user.id = :userId
            """)
    Optional<CommunityMember> findByCommunityIdAndUserIdWithCommunity(
            @Param("communityId") Long communityId,
            @Param("userId") Long userId
    );

    @Query("""
            from CommunityMember member
            join fetch member.community
            where member.community.id in :communityIds
                and member.user.id = :userId
            """)
    List<CommunityMember> findByCommunityIdInAndUserIdWithCommunity(
            @Param("communityIds") Collection<Long> communityIds,
            @Param("userId") Long userId
    );

    @Query("""
            select member.community.id as communityId,
                   count(member.user.id) as onlineCount
            from CommunityMember member, Presence presence
            where member.user.id = presence.userId
                and member.community.id in :communityIds
                and member.status = :status
                and presence.online = true
            group by member.community.id
            """)
    List<CommunityOnlineCount> countOnlineMembersByCommunityIds(
            @Param("communityIds") Collection<Long> communityIds,
            @Param("status") CommunityMemberStatus status
    );

    interface CommunityOnlineCount {
        Long getCommunityId();

        long getOnlineCount();
    }
}
