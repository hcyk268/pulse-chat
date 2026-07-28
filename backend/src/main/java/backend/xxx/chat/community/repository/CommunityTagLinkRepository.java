package backend.xxx.chat.community.repository;

import java.util.Collection;
import java.util.List;

import backend.xxx.chat.community.model.CommunityTagLink;
import backend.xxx.chat.community.model.CommunityTagLinkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityTagLinkRepository extends JpaRepository<CommunityTagLink, CommunityTagLinkId> {

    @Query("""
            from CommunityTagLink link
            join fetch link.tag
            where link.community.id in :communityIds
            order by link.community.id, link.tag.name
            """)
    List<CommunityTagLink> findByCommunityIdInWithTag(@Param("communityIds") Collection<Long> communityIds);

    @Modifying
    @Query("delete from CommunityTagLink link where link.community.id = :communityId")
    void deleteByCommunityId(@Param("communityId") Long communityId);
}
