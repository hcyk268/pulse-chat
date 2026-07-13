package backend.xxx.chat.realtime.service;

import java.util.List;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.realtime.dto.TypingStatusRequest;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.model.TypingUpdatedEventData;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TypingServiceTest {

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private ConversationAccessPolicy conversationAccessPolicy;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @Mock
    private RealtimeValidator realtimeValidator;

    @InjectMocks
    private TypingService typingService;

    @Test
    void updateTypingPublishesToOtherParticipantsOnly() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        User carol = user(3L, "carol");
        Conversation conversation = conversation(10L);
        List<ConversationParticipant> participants = List.of(
                participant(conversation, alice),
                participant(conversation, bob),
                participant(conversation, carol)
        );

        when(userLookupService.getCurrentUser("alice")).thenReturn(alice);
        when(conversationAccessPolicy.requireParticipants(conversation.getId()))
                .thenReturn(participants);

        typingService.updateTyping(
                "alice",
                conversation.getId(),
                new TypingStatusRequest(true)
        );

        ArgumentCaptor<TypingUpdatedEventData> dataCaptor =
                ArgumentCaptor.forClass(TypingUpdatedEventData.class);

        verify(realtimeEventPublisher).sendToUser(
                eq("bob"),
                eq(RealtimeEventType.TYPING_UPDATED),
                eq(conversation.getId()),
                dataCaptor.capture()
        );
        verify(realtimeEventPublisher).sendToUser(
                eq("carol"),
                eq(RealtimeEventType.TYPING_UPDATED),
                eq(conversation.getId()),
                dataCaptor.capture()
        );
        verify(realtimeEventPublisher, never()).sendToUser(
                eq("alice"),
                any(RealtimeEventType.class),
                any(Long.class),
                any()
        );

        assertThat(dataCaptor.getAllValues())
                .allSatisfy(data -> {
                    assertThat(data.userId()).isEqualTo(alice.getId());
                    assertThat(data.username()).isEqualTo(alice.getUsername());
                    assertThat(data.typing()).isTrue();
                });
    }

    @Test
    void updateTypingRejectsMissingConversation() {
        User alice = user(1L, "alice");
        Long conversationId = 10L;

        when(userLookupService.getCurrentUser("alice")).thenReturn(alice);
        when(conversationAccessPolicy.requireParticipants(conversationId))
                .thenThrow(new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Conversation not found"
                ));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> typingService.updateTyping(
                        "alice",
                        conversationId,
                        new TypingStatusRequest(true)
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        verifyNoInteractions(realtimeEventPublisher);
    }

    @Test
    void updateTypingRejectsNonParticipant() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        Conversation conversation = conversation(10L);
        List<ConversationParticipant> participants = List.of(participant(conversation, bob));

        when(userLookupService.getCurrentUser("alice")).thenReturn(alice);
        when(conversationAccessPolicy.requireParticipants(conversation.getId()))
                .thenReturn(participants);
        doThrow(new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "You are not allowed to update typing status for this conversation"
        )).when(conversationAccessPolicy).assertCanUpdateTyping(alice, participants);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> typingService.updateTyping(
                        "alice",
                        conversation.getId(),
                        new TypingStatusRequest(true)
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(realtimeEventPublisher);
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
