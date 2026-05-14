package backend.xxx.chat.message.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.message.dto.MessageHistoryResponse;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getHistoryReturnsEachPageOldestToNewestWhileCursorLoadsOlderMessages() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));

        saveMessage(conversation, bob, "message 1", Instant.parse("2026-01-01T00:00:00Z"));
        saveMessage(conversation, alice, "message 2", Instant.parse("2026-01-01T00:01:00Z"));
        saveMessage(conversation, bob, "message 3", Instant.parse("2026-01-01T00:02:00Z"));
        saveMessage(conversation, alice, "message 4", Instant.parse("2026-01-01T00:03:00Z"));
        entityManager.flush();
        entityManager.clear();

        MessageHistoryResponse firstPage = messageService.getHistory(
                alice.getUsername(),
                conversation.getId(),
                (short) 2,
                null
        );

        assertThat(firstPage.items())
                .extracting(MessageResponse::content)
                .containsExactly("message 3", "message 4");
        assertThat(firstPage.paging().hasMore()).isTrue();
        assertThat(firstPage.paging().nextCursor()).isNotBlank();

        MessageHistoryResponse secondPage = messageService.getHistory(
                alice.getUsername(),
                conversation.getId(),
                (short) 2,
                firstPage.paging().nextCursor()
        );

        assertThat(secondPage.items())
                .extracting(MessageResponse::content)
                .containsExactly("message 1", "message 2");
        assertThat(secondPage.paging().hasMore()).isFalse();
        assertThat(secondPage.paging().nextCursor()).isNull();
    }

    private Message saveMessage(
            Conversation conversation,
            User sender,
            String content,
            Instant createdAt
    ) {
        Message message = messageRepository.saveAndFlush(
                Message.createTextMessage(conversation, sender, UUID.randomUUID(), content)
        );
        entityManager.createNativeQuery("update messages set created_at = ? where id = ?")
                .setParameter(1, Timestamp.from(createdAt))
                .setParameter(2, message.getId())
                .executeUpdate();
        message.setCreatedAt(createdAt);
        return message;
    }
}
