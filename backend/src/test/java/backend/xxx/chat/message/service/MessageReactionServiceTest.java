package backend.xxx.chat.message.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.message.dto.MessageReactionGroupResponse;
import backend.xxx.chat.message.dto.MessageReactionRequest;
import backend.xxx.chat.message.dto.MessageReactionResponse;
import backend.xxx.chat.message.dto.MessageReactionsResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageReactionEmoji;
import backend.xxx.chat.message.repository.MessageReactionRepository;
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
class MessageReactionServiceTest {

    @Autowired
    private MessageReactionService messageReactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReactionRepository messageReactionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void reactMessageCreatesReactionAndReturnsExistingReactionWhenCalledAgain() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, bob, "message to react", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        MessageReactionRequest request = new MessageReactionRequest(MessageReactionEmoji.LIKE);

        MessageReactionService.ReactMessageResult firstResult =
                messageReactionService.reactMessage(alice.getUsername(), message.getId(), request);
        MessageReactionService.ReactMessageResult secondResult =
                messageReactionService.reactMessage(alice.getUsername(), message.getId(), request);

        MessageReactionResponse firstResponse = firstResult.response();
        MessageReactionResponse secondResponse = secondResult.response();
        assertThat(firstResult.created()).isTrue();
        assertThat(secondResult.created()).isFalse();
        assertThat(secondResponse).isEqualTo(firstResponse);
        assertThat(firstResponse.messageId()).isEqualTo(message.getId());
        assertThat(firstResponse.emoji()).isEqualTo(MessageReactionEmoji.LIKE);
        assertThat(firstResponse.reactedBy().id()).isEqualTo(alice.getId());
        assertThat(firstResponse.reactedAt()).isNotNull();
        assertThat(messageReactionRepository.findByMessageIdWithUser(message.getId())).hasSize(1);
    }

    @Test
    void getReactionsGroupsByEmojiAndMarksCurrentUserReaction() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, bob, "message to react", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        messageReactionService.reactMessage(
                alice.getUsername(),
                message.getId(),
                new MessageReactionRequest(MessageReactionEmoji.LIKE)
        );
        messageReactionService.reactMessage(
                bob.getUsername(),
                message.getId(),
                new MessageReactionRequest(MessageReactionEmoji.LIKE)
        );
        messageReactionService.reactMessage(
                bob.getUsername(),
                message.getId(),
                new MessageReactionRequest(MessageReactionEmoji.LOVE)
        );

        MessageReactionsResponse response = messageReactionService.getReactions(alice.getUsername(), message.getId());

        assertThat(response.messageId()).isEqualTo(message.getId());
        assertThat(response.items())
                .extracting(MessageReactionGroupResponse::emoji)
                .containsExactly(MessageReactionEmoji.LIKE, MessageReactionEmoji.LOVE);

        MessageReactionGroupResponse likeGroup = findGroup(response, MessageReactionEmoji.LIKE);
        assertThat(likeGroup.count()).isEqualTo(2);
        assertThat(likeGroup.reactedByMe()).isTrue();
        assertThat(likeGroup.users())
                .extracting("id")
                .containsExactly(alice.getId(), bob.getId());

        MessageReactionGroupResponse loveGroup = findGroup(response, MessageReactionEmoji.LOVE);
        assertThat(loveGroup.count()).isEqualTo(1);
        assertThat(loveGroup.reactedByMe()).isFalse();
    }

    @Test
    void removeReactionDeletesOnlyCurrentUserReactionForRequestedEmoji() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        Conversation conversation = conversationRepository.save(Conversation.createDirectConversation());
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, alice, true));
        conversationParticipantRepository.save(ConversationParticipant.create(conversation, bob, true));
        Message message = saveMessage(conversation, bob, "message to react", Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        messageReactionService.reactMessage(
                alice.getUsername(),
                message.getId(),
                new MessageReactionRequest(MessageReactionEmoji.LIKE)
        );
        messageReactionService.reactMessage(
                alice.getUsername(),
                message.getId(),
                new MessageReactionRequest(MessageReactionEmoji.LOVE)
        );
        messageReactionService.reactMessage(
                bob.getUsername(),
                message.getId(),
                new MessageReactionRequest(MessageReactionEmoji.LIKE)
        );

        messageReactionService.removeReaction(alice.getUsername(), message.getId(), MessageReactionEmoji.LIKE);
        messageReactionService.removeReaction(alice.getUsername(), message.getId(), MessageReactionEmoji.LIKE);

        MessageReactionsResponse response = messageReactionService.getReactions(alice.getUsername(), message.getId());
        MessageReactionGroupResponse likeGroup = findGroup(response, MessageReactionEmoji.LIKE);
        MessageReactionGroupResponse loveGroup = findGroup(response, MessageReactionEmoji.LOVE);

        assertThat(likeGroup.count()).isEqualTo(1);
        assertThat(likeGroup.reactedByMe()).isFalse();
        assertThat(likeGroup.users())
                .extracting("id")
                .containsExactly(bob.getId());
        assertThat(loveGroup.count()).isEqualTo(1);
        assertThat(loveGroup.reactedByMe()).isTrue();
    }

    private MessageReactionGroupResponse findGroup(
            MessageReactionsResponse response,
            MessageReactionEmoji emoji
    ) {
        return response.items()
                .stream()
                .filter(item -> item.emoji() == emoji)
                .findFirst()
                .orElseThrow();
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
