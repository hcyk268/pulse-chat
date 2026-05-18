package backend.xxx.chat.message.repository;

import java.util.Optional;
import java.util.List;

import backend.xxx.chat.message.model.MessagePin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessagePinRepository extends JpaRepository<MessagePin, Long> {

    long countByConversationId(Long conversationId);

    @Query("""
            from MessagePin messagePin
            join fetch messagePin.conversation
            join fetch messagePin.message message
            join fetch message.conversation
            join fetch message.sender
            join fetch messagePin.pinnedBy
            where messagePin.conversation.id = :conversationId
            order by messagePin.pinnedAt desc, messagePin.id desc
            """)
    List<MessagePin> findByConversationIdWithDetails(@Param("conversationId") Long conversationId);

    @Query("""
            from MessagePin messagePin
            join fetch messagePin.conversation
            join fetch messagePin.message message
            join fetch message.conversation
            join fetch message.sender
            join fetch messagePin.pinnedBy
            where messagePin.id = :messagePinId
            """)
    Optional<MessagePin> findByIdWithDetails(@Param("messagePinId") Long messagePinId);

    @Query("""
            from MessagePin messagePin
            join fetch messagePin.conversation
            join fetch messagePin.message message
            join fetch message.conversation
            join fetch message.sender
            join fetch messagePin.pinnedBy
            where messagePin.message.id = :messageId
            """)
    Optional<MessagePin> findByMessageIdWithDetails(@Param("messageId") Long messageId);
}
