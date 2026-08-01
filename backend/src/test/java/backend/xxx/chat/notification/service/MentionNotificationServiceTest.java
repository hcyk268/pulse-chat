package backend.xxx.chat.notification.service;

import java.util.List;
import java.util.Set;

import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.user.model.User;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MentionNotificationServiceTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final MentionNotificationService service =
            new MentionNotificationService(notificationService);

    @Test
    void extractsUniqueMentions() {
        Set<String> usernames = service.extractUsernames(
                "Hi @Alice, @bob.dev and again @ALICE. Email test@example.com is not a mention."
        );

        assertThat(usernames).containsExactly("alice", "bob.dev");
    }

    @Test
    void sendsAllMentionNotificationsInOneBatch() {
        User sender = user(1L, "alice");
        User bob = user(2L, "bob");
        User carol = user(3L, "carol");
        Conversation conversation = Conversation.createGroupConversation("group", null, sender);
        conversation.setId(10L);
        Message message = mock(Message.class);
        when(message.getId()).thenReturn(20L);
        when(message.getContent()).thenReturn("@bob and @carol");

        service.notifyMentions(
                message,
                sender,
                List.of(
                        ConversationParticipant.create(conversation, sender, true),
                        ConversationParticipant.create(conversation, bob, true),
                        ConversationParticipant.create(conversation, carol, true)
                )
        );

        verify(notificationService).createAll(argThat(commands ->
                commands.size() == 2
                        && commands.stream()
                                .map(command -> command.recipient().getUsername())
                                .toList()
                                .equals(List.of("bob", "carol"))
        ));
    }

    private User user(Long id, String username) {
        User user = User.create(
                username,
                username + "@example.com",
                "Password123!",
                username
        );
        user.setId(id);
        return user;
    }

    @Test
    void ignoresMissingMentions() {
        assertThat(service.extractUsernames("No mention here")).isEmpty();
        assertThat(service.extractUsernames(null)).isEmpty();
    }
}
