package backend.xxx.chat.message.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.dto.ConversationPinnedMessagesResponse;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.message.dto.AttachmentRequest;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.message.dto.EditMessageRequest;
import backend.xxx.chat.message.dto.MarkReadRequest;
import backend.xxx.chat.message.dto.MessageHistoryResponse;
import backend.xxx.chat.message.dto.MessagePinResponse;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.dto.SendMessageRequest;
import backend.xxx.chat.message.dto.UnPinMessageResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.message.repository.MessagePinRepository;
import backend.xxx.chat.message.repository.MessageReadRepository;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.repository.OutboxEventRepository;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.storage.model.UploadPurpose;
import backend.xxx.chat.storage.model.UploadSession;
import backend.xxx.chat.storage.model.UploadedAsset;
import backend.xxx.chat.storage.model.UploadedAssetStatus;
import backend.xxx.chat.storage.repository.UploadSessionRepository;
import backend.xxx.chat.storage.repository.UploadedAssetRepository;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private MessageReadRepository messageReadRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Autowired
    private UploadedAssetRepository uploadedAssetRepository;

    @Autowired
    private ObjectMapper objectMapper;

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
                        repliedMessage.getId(),
                        null
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
    void sendMessageWritesMessageCreatedOutboxPayload() throws Exception {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));

        MessageResponse response = messageService.sendMessage(
                alice.getUsername(),
                new SendMessageRequest(
                        conversation.getId(),
                        UUID.randomUUID(),
                        "hello outbox",
                        MessageType.TEXT,
                        null,
                        null
                )
        );

        OutboxEvent savedEvent = outboxEventRepository.findAll().get(0);
        JsonNode payload = objectMapper.readTree(savedEvent.getPayload());

        assertThat(savedEvent.getAggregateType()).isEqualTo("MESSAGE");
        assertThat(savedEvent.getAggregateId()).isEqualTo(response.id());
        assertThat(savedEvent.getEventType()).isEqualTo("message.created");
        assertThat(payload.get("conversationId").asLong()).isEqualTo(conversation.getId());
        assertThat(payload.get("messageId").asLong()).isEqualTo(response.id());
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
                        repliedMessage.getId(),
                        null
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
                        repliedMessage.getId(),
                        null
                )
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Reply message must belong to the same conversation");
    }

    @Test
    void sendMediaMessageStoresAttachmentsAndOptionalCaption() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));

        UploadedAsset photoAsset = createReadyMessageAsset(
                alice,
                "message-attachments/20260708/photo-1.png",
                "https://cdn.example.com/message-attachments/20260708/photo-1.png",
                "photo-1.png",
                "image/png",
                1200L
        );
        UploadedAsset videoAsset = createReadyMessageAsset(
                alice,
                "message-attachments/20260708/clip-1.mp4",
                "https://cdn.example.com/message-attachments/20260708/clip-1.mp4",
                "clip-1.mp4",
                "video/mp4",
                3200L
        );

        MessageResponse response = messageService.sendMessage(
                alice.getUsername(),
                new SendMessageRequest(
                        conversation.getId(),
                        UUID.randomUUID(),
                        "album moi",
                        MessageType.MEDIA,
                        null,
                        List.of(
                                new AttachmentRequest(photoAsset.getId()),
                                new AttachmentRequest(videoAsset.getId())
                        )
                )
        );

        assertThat(response.messageType()).isEqualTo(MessageType.MEDIA);
        assertThat(response.content()).isEqualTo("album moi");
        assertThat(response.attachments()).hasSize(2);
        assertThat(response.attachments().get(0).objectKey())
                .isEqualTo("message-attachments/20260708/photo-1.png");
        assertThat(response.attachments().get(1).fileName())
                .isEqualTo("clip-1.mp4");

        Message savedMessage = messageRepository.findById(response.id()).orElseThrow();
        assertThat(savedMessage.getAttachments()).hasSize(2);
        assertThat(savedMessage.getAttachments())
                .extracting("fileName")
                .containsExactly("photo-1.png", "clip-1.mp4");
        assertThat(uploadedAssetRepository.findById(photoAsset.getId()).orElseThrow().getStatus())
                .isEqualTo(UploadedAssetStatus.ATTACHED);
        assertThat(uploadedAssetRepository.findById(videoAsset.getId()).orElseThrow().getStatus())
                .isEqualTo(UploadedAssetStatus.ATTACHED);
    }

    @Test
    void sendMediaMessageRejectsMissingAttachments() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));

        assertThatThrownBy(() -> messageService.sendMessage(
                alice.getUsername(),
                new SendMessageRequest(
                        conversation.getId(),
                        UUID.randomUUID(),
                        null,
                        MessageType.MEDIA,
                        null,
                        List.of()
                )
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Media message must contain at least one attachment");
    }

    @Test
    void pinMessageCreatesPinAndReturnsExistingPinWhenCalledAgain() throws Exception {
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

        OutboxEvent savedEvent = onlyOutboxEvent();
        JsonNode payload = objectMapper.readTree(savedEvent.getPayload());
        assertThat(savedEvent.getAggregateType()).isEqualTo("MESSAGE_PIN");
        assertThat(savedEvent.getAggregateId()).isEqualTo(firstResponse.pinId());
        assertThat(savedEvent.getEventType()).isEqualTo(RealtimeEventType.MESSAGE_PINNED.getValue());
        assertThat(payload.get("conversationId").asLong()).isEqualTo(conversation.getId());
        assertThat(payload.get("messagePinId").asLong()).isEqualTo(firstResponse.pinId());
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
    void unPinMessageDeletesPinAndReturnsOriginalMessageId() throws Exception {
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

        OutboxEvent savedEvent = lastOutboxEvent();
        JsonNode payload = objectMapper.readTree(savedEvent.getPayload());
        assertThat(savedEvent.getAggregateType()).isEqualTo("MESSAGE");
        assertThat(savedEvent.getAggregateId()).isEqualTo(targetMessage.getId());
        assertThat(savedEvent.getEventType()).isEqualTo(RealtimeEventType.MESSAGE_UNPINNED.getValue());
        assertThat(payload.get("conversationId").asLong()).isEqualTo(conversation.getId());
        assertThat(payload.get("messageId").asLong()).isEqualTo(targetMessage.getId());
        assertThat(payload.get("unPinnedAt").asText()).isNotBlank();
    }

    @Test
    void getPinnedMessagesReturnsAllPinnedMessagesNewestFirst() {
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

        ConversationPinnedMessagesResponse response = messageService.getPinnedMessages(alice.getUsername(), conversation.getId());

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
    void editMessageUpdatesOwnTextMessageAndReturnsEditedAt() throws Exception {
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

        OutboxEvent savedEvent = onlyOutboxEvent();
        JsonNode payload = objectMapper.readTree(savedEvent.getPayload());
        assertThat(savedEvent.getAggregateType()).isEqualTo("MESSAGE");
        assertThat(savedEvent.getAggregateId()).isEqualTo(message.getId());
        assertThat(savedEvent.getEventType()).isEqualTo(RealtimeEventType.MESSAGE_UPDATED.getValue());
        assertThat(payload.get("conversationId").asLong()).isEqualTo(conversation.getId());
        assertThat(payload.get("messageId").asLong()).isEqualTo(message.getId());
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
    void deleteMessageSoftDeletesOwnMessageAndHidesContent() throws Exception {
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

        OutboxEvent savedEvent = onlyOutboxEvent();
        JsonNode payload = objectMapper.readTree(savedEvent.getPayload());
        assertThat(savedEvent.getAggregateType()).isEqualTo("MESSAGE");
        assertThat(savedEvent.getAggregateId()).isEqualTo(message.getId());
        assertThat(savedEvent.getEventType()).isEqualTo(RealtimeEventType.MESSAGE_DELETED.getValue());
        assertThat(payload.get("conversationId").asLong()).isEqualTo(conversation.getId());
        assertThat(payload.get("messageId").asLong()).isEqualTo(message.getId());
    }

    @Test
    void readMessageWritesMessageReadOutboxPayload() throws Exception {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        ConversationParticipant aliceParticipant =
                conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, bob, "message to read", Instant.parse("2026-01-01T00:00:00Z"));
        aliceParticipant.incrementUnreadCount();
        entityManager.flush();
        entityManager.clear();

        messageService.readMessage(
                alice.getUsername(),
                new MarkReadRequest(conversation.getId(), message.getId())
        );

        OutboxEvent savedEvent = onlyOutboxEvent();
        JsonNode payload = objectMapper.readTree(savedEvent.getPayload());
        assertThat(savedEvent.getAggregateType()).isEqualTo("MESSAGE");
        assertThat(savedEvent.getAggregateId()).isEqualTo(message.getId());
        assertThat(savedEvent.getEventType()).isEqualTo(RealtimeEventType.MESSAGE_READ.getValue());
        assertThat(payload.get("conversationId").asLong()).isEqualTo(conversation.getId());
        assertThat(payload.get("readerId").asLong()).isEqualTo(alice.getId());
        assertThat(payload.get("lastReadMessageId").asLong()).isEqualTo(message.getId());
        assertThat(payload.get("readAt").asText()).isNotBlank();
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


    @Test
    void pendingGroupMemberCannotSendOrReadMessages() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        User carol = userRepository.save(User.create("carol", "carol@example.com", "hashed-password", "Carol"));
        Conversation conversation = conversationRepository.save(Conversation.createGroupConversation("Team", null, alice));
        ConversationParticipant owner = ConversationParticipant.create(conversation, alice, true);
        owner.promoteToOwner();
        conversationParticipantRepository.save(owner);
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        conversationParticipantRepository.save(ConversationParticipant.createPending(conversation, carol, alice));
        Message message = saveMessage(conversation, alice, "group message", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> messageService.sendMessage(
                carol.getUsername(),
                new SendMessageRequest(
                        conversation.getId(),
                        UUID.randomUUID(),
                        "pending cannot send",
                        MessageType.TEXT,
                        null,
                        null
                )
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("You are not allowed to send message to this conversation");

        assertThatThrownBy(() -> messageService.readMessage(
                carol.getUsername(),
                new MarkReadRequest(conversation.getId(), message.getId())
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("You are not allowed to access this conversation");
    }

    @Test
    void leftGroupMemberDoesNotBecomeVisibleOrUnreadWhenMessageSent() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        User carol = userRepository.save(User.create("carol", "carol@example.com", "hashed-password", "Carol"));
        Conversation conversation = conversationRepository.save(Conversation.createGroupConversation("Team", null, alice));
        ConversationParticipant owner = ConversationParticipant.create(conversation, alice, true);
        owner.promoteToOwner();
        conversationParticipantRepository.save(owner);
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        ConversationParticipant leftParticipant = ConversationParticipant.create(conversation, carol, false);
        leftParticipant.markLeft(Instant.parse("2026-01-01T00:00:00Z"));
        leftParticipant.hideFromList();
        conversationParticipantRepository.save(leftParticipant);
        entityManager.flush();
        entityManager.clear();

        messageService.sendMessage(
                alice.getUsername(),
                new SendMessageRequest(
                        conversation.getId(),
                        UUID.randomUUID(),
                        "hello active members",
                        MessageType.TEXT,
                        null,
                        null
                )
        );
        entityManager.flush();
        entityManager.clear();

        ConversationParticipant bobParticipant = conversationParticipantRepository.findById(
                new ConversationParticipantId(conversation.getId(), bob.getId())
        ).orElseThrow();
        ConversationParticipant carolParticipant = conversationParticipantRepository.findById(
                new ConversationParticipantId(conversation.getId(), carol.getId())
        ).orElseThrow();

        assertThat(bobParticipant.getUnreadCount()).isEqualTo(1L);
        assertThat(bobParticipant.isVisibleInList()).isTrue();
        assertThat(carolParticipant.getUnreadCount()).isZero();
        assertThat(carolParticipant.isVisibleInList()).isFalse();
        assertThat(carolParticipant.isLeft()).isTrue();
    }

    @Test
    void groupReadStoresPerUserReadReceiptWithoutSettingGlobalMessageReadStatus() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createGroupConversation("Team", null, alice));
        ConversationParticipant owner = ConversationParticipant.create(conversation, alice, true);
        owner.promoteToOwner();
        conversationParticipantRepository.save(owner);
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, alice, "message to read", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        messageService.readMessage(
                bob.getUsername(),
                new MarkReadRequest(conversation.getId(), message.getId())
        );
        entityManager.flush();
        entityManager.clear();

        Message savedMessage = messageRepository.findById(message.getId()).orElseThrow();
        assertThat(savedMessage.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(savedMessage.getReadAt()).isNull();
        assertThat(messageReadRepository.findByMessageIdWithUserOrderByReadAtAsc(message.getId()))
                .singleElement()
                .satisfies(read -> {
                    assertThat(read.getUser().getId()).isEqualTo(bob.getId());
                    assertThat(read.getReadAt()).isNotNull();
                });
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

    private UploadedAsset createReadyMessageAsset(
            User owner,
            String objectKey,
            String publicUrl,
            String fileName,
            String contentType,
            Long sizeBytes
    ) {
        UploadSession session = uploadSessionRepository.saveAndFlush(UploadSession.create(
                owner,
                UploadPurpose.MESSAGE_ATTACHMENT,
                fileName,
                contentType,
                sizeBytes,
                5L * 1024 * 1024,
                1,
                objectKey,
                "r2-upload-" + UUID.randomUUID(),
                null,
                Instant.now().plusSeconds(3600)
        ));
        session.markVerified();

        return uploadedAssetRepository.saveAndFlush(UploadedAsset.readyFromSession(
                session,
                publicUrl,
                sizeBytes
        ));
    }

    private OutboxEvent onlyOutboxEvent() {
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        return events.get(0);
    }

    private OutboxEvent lastOutboxEvent() {
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).isNotEmpty();
        return events.stream()
                .max((left, right) -> left.getId().compareTo(right.getId()))
                .orElseThrow();
    }
}
