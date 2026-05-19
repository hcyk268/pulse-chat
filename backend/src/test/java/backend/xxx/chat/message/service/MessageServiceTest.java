package backend.xxx.chat.message.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.dto.ConversationPinsResponse;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.message.dto.EditMessageRequest;
import backend.xxx.chat.message.dto.MessageHistoryResponse;
import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.dto.SendMessageRequest;
import backend.xxx.chat.message.dto.UnPinMessageResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageType;
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
    void sendMessageCanReplyToMessageInSameConversation() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message repliedMessage = saveMessage(conversation, bob, "message being replied", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        MessageResponse response = messageService.sendMessage(
                alice.getUsername(),
                new SendMessageRequest(
                        conversation.getId(),
                        UUID.randomUUID(),
                        "reply content",
                        MessageType.TEXT,
                        repliedMessage.getId()
                )
        );

        assertThat(response.content()).isEqualTo("reply content");
        assertThat(response.replyTo()).isNotNull();
        assertThat(response.replyTo().id()).isEqualTo(repliedMessage.getId());
        assertThat(response.replyTo().content()).isEqualTo("message being replied");
        assertThat(response.replyTo().sender().id()).isEqualTo(bob.getId());

        Message savedReply = messageRepository.findById(response.id()).orElseThrow();
        assertThat(savedReply.getReplyToMessage().getId()).isEqualTo(repliedMessage.getId());
    }

    @Test
    void sendMessageRejectsDeletedReplyMessage() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message repliedMessage = saveMessage(conversation, bob, "deleted reply target", Instant.parse("2026-01-01T00:00:00Z"));
        repliedMessage.deleteForEveryone(bob, Instant.parse("2026-01-01T00:01:00Z"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> messageService.sendMessage(
                alice.getUsername(),
                new SendMessageRequest(
                        conversation.getId(),
                        UUID.randomUUID(),
                        "reply content",
                        MessageType.TEXT,
                        repliedMessage.getId()
                )
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Deleted message cannot be replied");
    }

    @Test
    void sendMessageRejectsReplyMessageFromDifferentConversation() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Conversation otherConversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(otherConversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(otherConversation, bob, true));
        Message repliedMessage = saveMessage(otherConversation, bob, "other conversation message", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> messageService.sendMessage(
                alice.getUsername(),
                new SendMessageRequest(
                        conversation.getId(),
                        UUID.randomUUID(),
                        "reply content",
                        MessageType.TEXT,
                        repliedMessage.getId()
                )
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Reply message must belong to the same conversation");
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
        assertThat(secondResponse.pinId()).isEqualTo(firstResponse.pinId());
        assertThat(firstResponse.message().conversationId()).isEqualTo(conversation.getId());
        assertThat(firstResponse.message().id()).isEqualTo(message.getId());
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

    @Test
    void unPinMessageDeletesPinAndReturnsOriginalMessageId() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        saveMessage(conversation, bob, "message before target", Instant.parse("2026-01-01T00:00:00Z"));
        Message targetMessage = saveMessage(conversation, bob, "message to unpin", Instant.parse("2026-01-01T00:01:00Z"));
        entityManager.flush();
        entityManager.clear();

        MessagePinResponse pinResponse = messageService.pinMessage(alice.getUsername(), targetMessage.getId()).response();

        assertThat(pinResponse.pinId()).isNotEqualTo(targetMessage.getId());

        UnPinMessageResponse unPinResponse = messageService.unPinMessage(alice.getUsername(), targetMessage.getId());

        assertThat(unPinResponse.conversationId()).isEqualTo(conversation.getId());
        assertThat(unPinResponse.messageId()).isEqualTo(targetMessage.getId());
        assertThat(unPinResponse.unpinnedAt()).isNotNull();
        assertThat(messagePinRepository.findByMessageIdWithDetails(targetMessage.getId())).isEmpty();
    }

    @Test
    void getConversationPinsReturnsAllPinnedMessagesNewestFirst() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message firstMessage = saveMessage(conversation, bob, "first pinned message", Instant.parse("2026-01-01T00:00:00Z"));
        Message secondMessage = saveMessage(conversation, bob, "second pinned message", Instant.parse("2026-01-01T00:01:00Z"));
        Message unpinnedMessage = saveMessage(conversation, bob, "not pinned", Instant.parse("2026-01-01T00:02:00Z"));
        entityManager.flush();
        entityManager.clear();

        messageService.pinMessage(alice.getUsername(), firstMessage.getId());
        messageService.pinMessage(alice.getUsername(), secondMessage.getId());

        ConversationPinsResponse response = messageService.getConversationPins(alice.getUsername(), conversation.getId());

        assertThat(response.conversationId()).isEqualTo(conversation.getId());
        assertThat(response.items())
                .extracting(item -> item.message().id())
                .containsExactly(secondMessage.getId(), firstMessage.getId());
        assertThat(response.items())
                .extracting(MessagePinResponse::pinnedAt)
                .doesNotContainNull();
        assertThat(response.items())
                .extracting(item -> item.message().id())
                .doesNotContain(unpinnedMessage.getId());
    }

    @Test
    void editMessageUpdatesOwnTextMessageAndReturnsEditedAt() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, alice, "original content", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        MessageResponse response = messageService.editMessage(
                alice.getUsername(),
                message.getId(),
                new EditMessageRequest(" edited content ", MessageType.TEXT)
        );

        assertThat(response.id()).isEqualTo(message.getId());
        assertThat(response.content()).isEqualTo("edited content");
        assertThat(response.editedAt()).isNotNull();

        Message savedMessage = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(savedMessage.getContent()).isEqualTo("edited content");
        assertThat(savedMessage.getEditedAt()).isNotNull();
    }

    @Test
    void editMessageRejectsWhenCurrentUserIsNotSender() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, bob, "bob content", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> messageService.editMessage(
                alice.getUsername(),
                message.getId(),
                new EditMessageRequest("alice edit", MessageType.TEXT)
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Only message sender can edit this message");
    }

    @Test
    void editMessageRejectsDeletedMessage() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, alice, "deleted content", Instant.parse("2026-01-01T00:00:00Z"));
        message.deleteForEveryone(alice, Instant.parse("2026-01-01T00:01:00Z"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> messageService.editMessage(
                alice.getUsername(),
                message.getId(),
                new EditMessageRequest("edited deleted content", MessageType.TEXT)
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Deleted message cannot be edited");
    }

    @Test
    void deleteMessageSoftDeletesOwnMessageAndHidesContent() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, alice, "content to recall", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        MessageResponse response = messageService.deleteMessage(alice.getUsername(), message.getId());

        assertThat(response.id()).isEqualTo(message.getId());
        assertThat(response.content()).isNull();
        assertThat(response.deletedAt()).isNotNull();
        assertThat(response.deletedBy().id()).isEqualTo(alice.getId());

        Message savedMessage = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(savedMessage.getContent()).isEqualTo("content to recall");
        assertThat(savedMessage.getDeletedAt()).isNotNull();
        assertThat(savedMessage.getDeletedBy().getId()).isEqualTo(alice.getId());

        MessageHistoryResponse history = messageService.getHistory(alice.getUsername(), conversation.getId(), (short) 20, null);
        assertThat(history.items())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.id()).isEqualTo(message.getId());
                    assertThat(item.content()).isNull();
                    assertThat(item.deletedAt()).isNotNull();
                    assertThat(item.deletedBy().id()).isEqualTo(alice.getId());
                });
    }

    @Test
    void deleteMessageRejectsWhenCurrentUserIsNotSender() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, bob, "bob content", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> messageService.deleteMessage(alice.getUsername(), message.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Only message sender can delete this message");
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
