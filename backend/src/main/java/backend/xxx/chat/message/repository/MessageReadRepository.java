package backend.xxx.chat.message.repository;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageRead;
import backend.xxx.chat.message.model.MessageReadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageReadRepository extends JpaRepository<MessageRead, MessageReadId> {

    @Query("""
            from MessageRead messageRead
            join fetch messageRead.user
            where messageRead.message.id = :messageId
            order by messageRead.readAt asc
            """)
    List<MessageRead> findByMessageIdWithUserOrderByReadAtAsc(@Param("messageId") Long messageId);

    @Query("""
            from Message message
            where message.conversation.id = :conversationId
                and message.sender.id <> :readerId
                and (
                    message.createdAt < :lastReadCreatedAt
                    or (
                        message.createdAt = :lastReadCreatedAt
                        and message.id <= :lastReadMessageId
                    )
                )
                and not exists (
                    select 1
                    from MessageRead messageRead
                    where messageRead.message = message
                        and messageRead.user.id = :readerId
                )
            """)
    List<Message> findUnreadMessagesByReaderUpTo(
            @Param("conversationId") Long conversationId,
            @Param("readerId") Long readerId,
            @Param("lastReadCreatedAt") Instant lastReadCreatedAt,
            @Param("lastReadMessageId") Long lastReadMessageId
    );
}