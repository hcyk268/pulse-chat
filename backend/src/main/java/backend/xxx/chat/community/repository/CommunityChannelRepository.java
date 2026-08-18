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
            select channel.slug
            from CommunityChannel channel
            where channel.community.id = :communityId
                and (
                    channel.slug = :baseSlug
                    or channel.slug like concat(:baseSlug, '-%')
                )
            """)
    List<String> findSlugsByCommunityIdAndBase(
            @Param("communityId") Long communityId,
            @Param("baseSlug") String baseSlug
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
            join fetch channel.community community
            left join fetch community.avatarAsset
            join fetch channel.conversation
            where channel.id = :channelId
            """)
    Optional<CommunityChannel> findByIdWithCommunityAndConversation(@Param("channelId") Long channelId);

    @Query("""
            from CommunityChannel channel
            join fetch channel.community
            where channel.conversation.id = :conversationId
            """)
    Optional<CommunityChannel> findByConversationIdWithCommunity(
            @Param("conversationId") Long conversationId
    );

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
