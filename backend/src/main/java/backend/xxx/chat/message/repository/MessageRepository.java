package backend.xxx.chat.message.repository;

import java.util.Collection;
import java.util.List;

import backend.xxx.chat.message.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            from Message message
            join fetch message.sender
            where message.id in :messageIds
            """)
    List<Message> findByIdInWithSender(@Param("messageIds") Collection<Long> messageIds);
}
