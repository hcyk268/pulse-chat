package backend.xxx.chat.message.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.message.dto.MessageHistoryResponse;
import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.repository.MessagePinRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private MessagePinRepository messagePinRepository;

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

    @Test
    void pinMessageCreatesPinAndReturnsExistingPinWhenCalledAgain() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, bob, "message to pin", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        MessageService.PinMessageResult firstResult = messageService.pinMessage(alice.getUsername(), message.getId());
        MessageService.PinMessageResult secondResult = messageService.pinMessage(alice.getUsername(), message.getId());

        MessagePinResponse firstResponse = firstResult.response();
        MessagePinResponse secondResponse = secondResult.response();
        assertThat(firstResult.created()).isTrue();
        assertThat(secondResult.created()).isFalse();
        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(firstResponse.conversationId()).isEqualTo(conversation.getId());
        assertThat(firstResponse.messageId()).isEqualTo(message.getId());
        assertThat(firstResponse.pinnedBy().id()).isEqualTo(alice.getId());
        assertThat(messagePinRepository.countByConversationId(conversation.getId())).isEqualTo(1L);
    }

    @Test
    void pinMessageRejectsWhenConversationAlreadyHasTwentyPins() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));

        Instant startAt = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < 20; i++) {
            Message message = saveMessage(conversation, bob, "pinned message " + i, startAt.plusSeconds(i));
            messageService.pinMessage(alice.getUsername(), message.getId());
        }

        Message overflowMessage = saveMessage(conversation, bob, "overflow message", startAt.plusSeconds(60));

        assertThatThrownBy(() -> messageService.pinMessage(alice.getUsername(), overflowMessage.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Conversation can only have 20 pinned messages");
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
