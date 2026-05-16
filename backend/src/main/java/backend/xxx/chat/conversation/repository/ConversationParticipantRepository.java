package backend.xxx.chat.conversation.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.model.ConversationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

    @Query("""
            select targetParticipant.user.id as userId,
                max(targetParticipant.conversation.id) as conversationId
            from ConversationParticipant currentParticipant, ConversationParticipant targetParticipant
            where currentParticipant.conversation = targetParticipant.conversation
                and currentParticipant.user.id = :currentUserId
                and currentParticipant.isVisibleInList = true
                and targetParticipant.user.id in :targetUserIds
                and currentParticipant.conversation.type = :conversationType
                and targetParticipant.user.id <> :currentUserId
            group by targetParticipant.user.id
            """)
    List<DirectConversationLookup> findDirectConversationIds(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserIds") Collection<Long> targetUserIds,
            @Param("conversationType") ConversationType conversationType
    );

    @Query("""
            select max(candidate.conversation.id)
            from ConversationParticipant candidate
            where candidate.conversation.type = :conversationType
                and candidate.conversation.id in (
                    select participant.conversation.id
                    from ConversationParticipant participant
                    where participant.conversation.type = :conversationType
                        and participant.user.id in (:currentUserId, :targetUserId)
                    group by participant.conversation.id
                    having count(distinct participant.user.id) = 2
                )
            """)
    Optional<Long> findDirectConversationIdBetween(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserId") Long targetUserId,
            @Param("conversationType") ConversationType conversationType
    );

    @Query("""
            from ConversationParticipant participant
            join fetch participant.user
            where participant.conversation.id = :conversationId
            order by participant.user.id
            """)
    List<ConversationParticipant> findByConversationIdWithUser(@Param("conversationId") Long conversationId);

    @Query("""
        from ConversationParticipant cp
        join fetch cp.conversation
        where cp.user.id = :userId
            and cp.isVisibleInList = true
            and coalesce(cp.conversation.lastMessageAt, cp.conversation.createdAt) <= :snapshotAt
        order by coalesce(cp.conversation.lastMessageAt, cp.conversation.createdAt) desc,
                 cp.conversation.id desc
        """)
    List<ConversationParticipant> findVisibleFirstPageByUserId(
            @Param("userId") Long userId,
            @Param("snapshotAt") Instant snapshotAt,
            Pageable pageable
    );

    @Query("""
        from ConversationParticipant cp
        join fetch cp.conversation
        where cp.user.id = :userId
            and cp.isVisibleInList = true
            and coalesce(cp.conversation.lastMessageAt, cp.conversation.createdAt) <= :snapshotAt
            and (
                coalesce(cp.conversation.lastMessageAt, cp.conversation.createdAt) < :cursorAt
                or (
                    coalesce(cp.conversation.lastMessageAt, cp.conversation.createdAt) = :cursorAt
                    and cp.conversation.id < :cursorId
                )
            )
        order by coalesce(cp.conversation.lastMessageAt, cp.conversation.createdAt) desc,
                 cp.conversation.id desc
        """)
    List<ConversationParticipant> findVisiblePageByUserIdAfterCursor(
            @Param("userId") Long userId,
            @Param("snapshotAt") Instant snapshotAt,
            @Param("cursorAt") Instant cursorAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
        from ConversationParticipant participant
        join fetch participant.user
        where participant.conversation.id in :conversationIds
        order by participant.conversation.id, participant.user.id
        """)
    List<ConversationParticipant> findByConversationIdInWithUser(
            @Param("conversationIds") Collection<Long> conversationIds
    );

    @Query("""
        select distinct participant.user.username
        from ConversationParticipant participant
        where participant.user.id <> :userId
            and participant.isVisibleInList = true
            and participant.conversation.id in (
                select actorParticipant.conversation.id
                from ConversationParticipant actorParticipant
                where actorParticipant.user.id = :userId
            )
        """)
    List<String> findVisiblePeerUsernamesByUserId(@Param("userId") Long userId);

    interface DirectConversationLookup {
        Long getUserId();

        Long getConversationId();
    }
}
