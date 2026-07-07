package backend.xxx.chat.realtime.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.outbox.payload.MessageDeliveredOutboxPayload;
import backend.xxx.chat.outbox.service.OutBoxService;
import backend.xxx.chat.realtime.model.RealtimeEventType;
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
class DeliveredServiceTest {

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationAccessPolicy conversationAccessPolicy;

    @Mock
    private OutBoxService outBoxService;

    @InjectMocks
    private DeliveredService deliveredService;

    @Test
    void messageDeliveredMarksIncomingMessagesUpToCutoffAndWritesOutboxEvent() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        Conversation conversation = conversation(10L);
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Message message = message(100L, conversation, alice, createdAt);
        List<ConversationParticipant> participants = List.of(
                participant(conversation, alice),
                participant(conversation, bob)
        );

        when(userLookupService.getCurrentUser("bob")).thenReturn(bob);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(conversationAccessPolicy.requireParticipants(conversation.getId()))
                .thenReturn(participants);
        when(messageRepository.markMessagesDeliveredUpTo(
                eq(conversation.getId()),
                eq(bob.getId()),
                eq(createdAt),
                eq(message.getId()),
                eq(MessageStatus.SENT),
                eq(MessageStatus.DELIVERED),
                any(Instant.class)
        )).thenReturn(2);

        deliveredService.messageDelivered("bob", message.getId());

        ArgumentCaptor<Instant> deliveredAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(messageRepository).markMessagesDeliveredUpTo(
                eq(conversation.getId()),
                eq(bob.getId()),
                eq(createdAt),
                eq(message.getId()),
                eq(MessageStatus.SENT),
                eq(MessageStatus.DELIVERED),
                deliveredAtCaptor.capture()
        );

        ArgumentCaptor<MessageDeliveredOutboxPayload> payloadCaptor =
                ArgumentCaptor.forClass(MessageDeliveredOutboxPayload.class);
        verify(outBoxService).pushEvent(
                eq("MESSAGE"),
                eq(message.getId()),
                eq(RealtimeEventType.MESSAGE_STATUS_UPDATED.getValue()),
                payloadCaptor.capture()
        );

        MessageDeliveredOutboxPayload payload = payloadCaptor.getValue();
        assertThat(payload.conversationId()).isEqualTo(conversation.getId());
        assertThat(payload.receiverId()).isEqualTo(bob.getId());
        assertThat(payload.senderId()).isEqualTo(alice.getId());
        assertThat(payload.lastDeliveredMessageId()).isEqualTo(message.getId());
        assertThat(payload.deliveredAt()).isEqualTo(deliveredAtCaptor.getValue());
    }

    @Test
    void messageDeliveredRejectsNonParticipant() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        User carol = user(3L, "carol");
        Conversation conversation = conversation(10L);
        Message message = message(100L, conversation, alice, Instant.parse("2026-01-01T00:00:00Z"));
        List<ConversationParticipant> participants = List.of(
                participant(conversation, alice),
                participant(conversation, bob)
        );

        when(userLookupService.getCurrentUser("carol")).thenReturn(carol);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(conversationAccessPolicy.requireParticipants(conversation.getId()))
                .thenReturn(participants);
        doThrow(new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "You are not allowed to update this message status"
        )).when(conversationAccessPolicy).assertCanUpdateMessageStatus(carol, participants);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> deliveredService.messageDelivered("carol", message.getId())
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(messageRepository, never()).markMessagesDeliveredUpTo(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verifyNoInteractions(outBoxService);
    }

    @Test
    void messageDeliveredRejectsSender() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        Conversation conversation = conversation(10L);
        Message message = message(100L, conversation, alice, Instant.parse("2026-01-01T00:00:00Z"));
        List<ConversationParticipant> participants = List.of(
                participant(conversation, alice),
                participant(conversation, bob)
        );

        when(userLookupService.getCurrentUser("alice")).thenReturn(alice);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(conversationAccessPolicy.requireParticipants(conversation.getId()))
                .thenReturn(participants);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> deliveredService.messageDelivered("alice", message.getId())
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(messageRepository, never()).markMessagesDeliveredUpTo(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verifyNoInteractions(outBoxService);
    }

    @Test
    void messageDeliveredDoesNotPublishEventWhenNothingChanged() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        Conversation conversation = conversation(10L);
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Message message = message(100L, conversation, alice, createdAt);
        List<ConversationParticipant> participants = List.of(
                participant(conversation, alice),
                participant(conversation, bob)
        );

        when(userLookupService.getCurrentUser("bob")).thenReturn(bob);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(conversationAccessPolicy.requireParticipants(conversation.getId()))
                .thenReturn(participants);
        when(messageRepository.markMessagesDeliveredUpTo(
                eq(conversation.getId()),
                eq(bob.getId()),
                eq(createdAt),
                eq(message.getId()),
                eq(MessageStatus.SENT),
                eq(MessageStatus.DELIVERED),
                any(Instant.class)
        )).thenReturn(0);

        deliveredService.messageDelivered("bob", message.getId());

        verifyNoInteractions(outBoxService);
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

    private Message message(
            Long id,
            Conversation conversation,
            User sender,
            Instant createdAt
    ) {
        Message message = Message.createTextMessage(
                conversation,
                sender,
                UUID.randomUUID(),
                "hello"
        );
        message.setId(id);
        message.setCreatedAt(createdAt);
        return message;
    }
}
