package backend.xxx.chat.conversation.repository;

import java.util.Optional;

import backend.xxx.chat.conversation.model.Conversation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            from Conversation conversation
            where conversation.id = :conversationId
            """)
    Optional<Conversation> findByIdForUpdate(@Param("conversationId") Long conversationId);
}
