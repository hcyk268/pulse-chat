package backend.xxx.chat.conversation.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import backend.xxx.chat.config.JpaAuditingConfig;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationType;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ConversationParticipantRepositoryTest {

    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findDirectConversationIdsReturnsOnlyVisibleDirectConversationsForCurrentUser() {
        User currentUser = persistUser("alice", "alice@example.com", "Alice");
        User visibleTargetUser = persistUser("bob", "bob@example.com", "Bob");
        User hiddenTargetUser = persistUser("charlie", "charlie@example.com", "Charlie");

        Conversation visibleConversation = persistDirectConversation();
        persistParticipant(visibleConversation, currentUser, true);
        persistParticipant(visibleConversation, visibleTargetUser, false);

        Conversation hiddenConversation = persistDirectConversation();
        persistParticipant(hiddenConversation, currentUser, false);
        persistParticipant(hiddenConversation, hiddenTargetUser, false);

        entityManager.flush();
        entityManager.clear();

        Map<Long, Long> conversationIdByUserId = conversationParticipantRepository.findDirectConversationIds(
                        currentUser.getId(),
                        List.of(visibleTargetUser.getId(), hiddenTargetUser.getId()),
                        ConversationType.DIRECT
                )
                .stream()
                .collect(Collectors.toMap(
                        ConversationParticipantRepository.DirectConversationLookup::getUserId,
                        ConversationParticipantRepository.DirectConversationLookup::getConversationId
                ));

        assertThat(conversationIdByUserId)
                .containsEntry(visibleTargetUser.getId(), visibleConversation.getId())
                .doesNotContainKey(hiddenTargetUser.getId());
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

    private void persistParticipant(Conversation conversation, User user, boolean isVisibleInList) {
        entityManager.persist(ConversationParticipant.create(conversation, user, isVisibleInList));
    }
}
