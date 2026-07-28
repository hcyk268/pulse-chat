package backend.xxx.chat.community.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.community.model.CommunityChannel;
import backend.xxx.chat.community.model.CommunityChannelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityChannelRepository extends JpaRepository<CommunityChannel, Long> {

    @Query("""
            select count(channel) > 0
            from CommunityChannel channel
            where channel.community.id = :communityId
                and channel.slug = :slug
            """)
    boolean existsByCommunityIdAndSlug(
            @Param("communityId") Long communityId,
            @Param("slug") String slug
    );

    @Query("""
            from CommunityChannel channel
            join fetch channel.conversation
            where channel.community.id = :communityId
                and channel.status = :status
            order by channel.sortOrder asc, channel.id asc
            """)
    List<CommunityChannel> findByCommunityIdAndStatusWithConversation(
            @Param("communityId") Long communityId,
            @Param("status") CommunityChannelStatus status
    );

    @Query("""
            from CommunityChannel channel
            join fetch channel.community
            join fetch channel.conversation
            where channel.id = :channelId
            """)
    Optional<CommunityChannel> findByIdWithCommunityAndConversation(@Param("channelId") Long channelId);

    @Query("""
            select coalesce(max(channel.sortOrder), 0)
            from CommunityChannel channel
            where channel.community.id = :communityId
            """)
    int findMaxSortOrderByCommunityId(@Param("communityId") Long communityId);

    @Query("""
            from CommunityChannel channel
            join fetch channel.conversation
            where channel.community.id = :communityId
                and channel.conversation.id in :conversationIds
            """)
    List<CommunityChannel> findByCommunityIdAndConversationIdInWithConversation(
            @Param("communityId") Long communityId,
            @Param("conversationIds") Collection<Long> conversationIds
    );
}
