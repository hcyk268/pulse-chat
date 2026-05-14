package backend.xxx.chat.conversation.service;

import java.time.Instant;
import java.util.UUID;

import backend.xxx.chat.common.exception.UserNotFoundException;
import backend.xxx.chat.conversation.dto.ConversationBoxResponse;
import backend.xxx.chat.conversation.dto.CreateDirectConversationRequest;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
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
class ConversationServiceTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void createOrOpenDirectConversationCreatesConversationVisibleOnlyForCreatorThenOpensForTarget() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));

        ConversationService.CreateOrOpenDirectConversationResult createdResult =
                conversationService.createOrOpenDirectConversation(
                        alice.getUsername(),
                        new CreateDirectConversationRequest(bob.getId())
                );

        Long conversationId = createdResult.response().id();

        assertThat(createdResult.created()).isTrue();
        assertThat(createdResult.response().otherParticipant().id()).isEqualTo(bob.getId());
        assertThat(conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversationId, alice.getId())
                ))
                .get()
                .extracting("visibleInList")
                .isEqualTo(true);
        assertThat(conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversationId, bob.getId())
                ))
                .get()
                .extracting("visibleInList")
                .isEqualTo(false);

        ConversationService.CreateOrOpenDirectConversationResult openedResult =
                conversationService.createOrOpenDirectConversation(
                        bob.getUsername(),
                        new CreateDirectConversationRequest(alice.getId())
                );

        assertThat(openedResult.created()).isFalse();
        assertThat(openedResult.response().id()).isEqualTo(conversationId);
        assertThat(conversationParticipantRepository.findById(
                        new ConversationParticipantId(conversationId, bob.getId())
                ))
                .get()
                .extracting("visibleInList")
                .isEqualTo(true);
    }

    @Test
    void createOrOpenDirectConversationThrowsUserNotFoundWhenTargetDoesNotExist() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));

        assertThatThrownBy(() -> conversationService.createOrOpenDirectConversation(
                alice.getUsername(),
                new CreateDirectConversationRequest(Long.MAX_VALUE)
        ))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void getConversationsReturnsEmptyPageWhenUserHasNoVisibleConversations() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));

        ConversationBoxResponse response = conversationService.getConversations(
                (short) 20,
                null,
                Instant.parse("2026-12-31T00:00:00Z"),
                alice.getUsername()
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.paging().hasMore()).isFalse();
        assertThat(response.paging().nextCursor()).isNull();
    }

    @Test
    void getConversationsReturnsVisibleConversationsWithCursorAndLastMessage() {
        User alice = userRepository.save(User.create("alice", "alice@example.com", "hashed-password", "Alice"));
        User bob = userRepository.save(User.create("bob", "bob@example.com", "hashed-password", "Bob"));
        User carol = userRepository.save(User.create("carol", "carol@example.com", "hashed-password", "Carol"));

        Conversation bobConversation = createVisibleDirectConversationWithMessage(
                alice,
                bob,
                "hello from bob",
                Instant.parse("2026-01-02T00:00:00Z")
        );
        Conversation carolConversation = createVisibleDirectConversationWithMessage(
                alice,
                carol,
                "newest message from carol",
                Instant.parse("2026-01-03T00:00:00Z")
        );

        ConversationBoxResponse firstPage = conversationService.getConversations(
                (short) 1,
                null,
                Instant.parse("2026-12-31T00:00:00Z"),
                alice.getUsername()
        );

        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.items().get(0).id()).isEqualTo(carolConversation.getId());
        assertThat(firstPage.items().get(0).otherParticipant().id()).isEqualTo(carol.getId());
        assertThat(firstPage.items().get(0).lastMessage().contentPreview()).isEqualTo("newest message from carol");
        assertThat(firstPage.paging().hasMore()).isTrue();
        assertThat(firstPage.paging().nextCursor()).isNotBlank();

        ConversationBoxResponse secondPage = conversationService.getConversations(
                (short) 1,
                firstPage.paging().nextCursor(),
                firstPage.paging().snapshotAt(),
                alice.getUsername()
        );

        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.items().get(0).id()).isEqualTo(bobConversation.getId());
        assertThat(secondPage.items().get(0).otherParticipant().id()).isEqualTo(bob.getId());
        assertThat(secondPage.paging().hasMore()).isFalse();
        assertThat(secondPage.paging().nextCursor()).isNull();
    }

    private Conversation createVisibleDirectConversationWithMessage(
            User currentUser,
            User otherUser,
            String content,
            Instant lastMessageAt
    ) {
        ConversationService.CreateOrOpenDirectConversationResult result =
                conversationService.createOrOpenDirectConversation(
                        currentUser.getUsername(),
                        new CreateDirectConversationRequest(otherUser.getId())
                );
        Conversation conversation = conversationRepository.findById(result.response().id()).orElseThrow();
        Message message = messageRepository.saveAndFlush(
                Message.createTextMessage(conversation, otherUser, UUID.randomUUID(), content)
        );

        conversation.updateLastMessage(message.getId(), lastMessageAt);
        return conversationRepository.saveAndFlush(conversation);
    }
}
