package backend.xxx.chat.conversation.service;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.repository.ConversationRepository;
import backend.xxx.chat.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationAccessPolicyTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @InjectMocks
    private ConversationAccessPolicy conversationAccessPolicy;

    @Test
    void requireParticipantsRejectsMissingConversation() {
        Long conversationId = 10L;

        when(conversationParticipantRepository.findByConversationIdWithUser(conversationId))
                .thenReturn(List.of());
        when(conversationRepository.existsById(conversationId)).thenReturn(false);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> conversationAccessPolicy.requireParticipants(conversationId)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void requireParticipantRejectsNonParticipant() {
        Long conversationId = 10L;
        Long userId = 1L;

        when(conversationParticipantRepository.findById(new ConversationParticipantId(conversationId, userId)))
                .thenReturn(Optional.empty());
        when(conversationRepository.existsById(conversationId)).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> conversationAccessPolicy.requireParticipant(conversationId, userId)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void assertCanSendMessageAllowsParticipant() {
        User alice = user(1L, "alice");
        Conversation conversation = conversation(10L);
        List<ConversationParticipant> participants = List.of(participant(conversation, alice));

        conversationAccessPolicy.assertCanSendMessage(alice, participants);
    }

    @Test
    void assertCanReadConversationRejectsNonParticipant() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        Conversation conversation = conversation(10L);
        List<ConversationParticipant> participants = List.of(participant(conversation, bob));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> conversationAccessPolicy.assertCanReadConversation(alice, participants)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    private User user(Long id, String username) {
        User user = User.create(
                username,
                username + "@example.com",
                "hashed-password",
                username
        );
        user.setId(id);
        return user;
    }

    private Conversation conversation(Long id) {
        Conversation conversation = Conversation.createDirectConversation();
        conversation.setId(id);
        return conversation;
    }

    private ConversationParticipant participant(Conversation conversation, User user) {
        return ConversationParticipant.create(conversation, user, true);
    }
}
