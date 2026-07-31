package backend.xxx.chat.notification.service;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MentionNotificationServiceTest {

    private final MentionNotificationService service =
            new MentionNotificationService(mock(NotificationService.class));

    @Test
    void extractsUniqueMentions() {
        Set<String> usernames = service.extractUsernames(
                "Hi @Alice, @bob.dev and again @ALICE. Email test@example.com is not a mention."
        );

        assertThat(usernames).containsExactly("alice", "bob.dev");
    }

    @Test
    void ignoresMissingMentions() {
        assertThat(service.extractUsernames("No mention here")).isEmpty();
        assertThat(service.extractUsernames(null)).isEmpty();
    }
}
