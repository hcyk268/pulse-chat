package backend.xxx.chat.message.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            from Message message
            join fetch message.sender
            where message.id in :messageIds
            """)
    List<Message> findByIdInWithSender(@Param("messageIds") Collection<Long> messageIds);

    @Query("""
        from Message m
        join fetch m.sender
        where m.conversation.id = :conversationId
          and (
              :cursorCreatedAt is null
              or m.createdAt < :cursorCreatedAt
              or (
                  m.createdAt = :cursorCreatedAt
                  and m.id < :cursorMessageId
              )
          )
        order by m.createdAt desc, m.id desc
        """)
    List<Message> findPageByConversationId(
            @Param("conversationId") Long conversationId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorMessageId") Long cursorMessageId,
            Pageable pageable
    );

    @Query("""
            from Message message
            join fetch message.sender
            where message.conversation.id = :conversationId
                and message.clientMessageId = :clientMessageId
            """)
    Optional<Message> findByConversationIdAndClientMessageIdWithSender(
            @Param("conversationId") Long conversationId,
            @Param("clientMessageId") UUID clientMessageId
    );

    @Query("""
            from Message message
            where message.id = :messageId
                and message.conversation.id = :conversationId
            """)
    Optional<Message> findByIdAndConversationId(
            @Param("messageId") Long messageId,
            @Param("conversationId") Long conversationId
    );

    @Modifying
    @Query("""
            update Message message
            set message.status = :status,
                message.deliveredAt = coalesce(message.deliveredAt, :readAt),
                message.readAt = coalesce(message.readAt, :readAt)
            where message.conversation.id = :conversationId
                and message.sender.id <> :readerId
                and message.readAt is null
                and (
                    message.createdAt < :lastReadCreatedAt
                    or (
                        message.createdAt = :lastReadCreatedAt
                        and message.id <= :lastReadMessageId
                    )
                )
            """)
    int markMessagesReadUpTo(
            @Param("conversationId") Long conversationId,
            @Param("readerId") Long readerId,
            @Param("lastReadCreatedAt") Instant lastReadCreatedAt,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("status") MessageStatus status,
            @Param("readAt") Instant readAt
    );

}
