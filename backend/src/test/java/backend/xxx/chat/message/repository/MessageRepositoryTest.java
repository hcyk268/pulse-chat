package backend.xxx.chat.message.repository;

import java.util.UUID;

import backend.xxx.chat.config.JpaAuditingConfig;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveRejectsDuplicateClientMessageIdWithinSameConversation() {
        User sender = persistUser("alice", "alice@example.com", "Alice");
        Conversation conversation = persistDirectConversation();
        UUID clientMessageId = UUID.randomUUID();

        messageRepository.saveAndFlush(
                Message.createTextMessage(conversation, sender, clientMessageId, "first message")
        );

        assertThatThrownBy(() -> messageRepository.saveAndFlush(
                Message.createTextMessage(conversation, sender, clientMessageId, "second message")
        ))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User persistUser(String username, String email, String displayName) {
        User user = User.create(username, email, "hashed-password", displayName);
        entityManager.persist(user);
        return user;
    }

    private Conversation persistDirectConversation() {
        Conversation conversation = Conversation.createDirectConversation();
        entityManager.persist(conversation);
        return conversation;
    }
}
