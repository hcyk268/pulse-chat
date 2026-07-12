package backend.xxx.chat.message.repository;

import java.util.List;

import backend.xxx.chat.message.model.MessageRead;
import backend.xxx.chat.message.model.MessageReadId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReadRepository extends JpaRepository<MessageRead, MessageReadId> {

    List<MessageRead> findByMessage_IdOrderByReadAtAsc(Long messageId);
}