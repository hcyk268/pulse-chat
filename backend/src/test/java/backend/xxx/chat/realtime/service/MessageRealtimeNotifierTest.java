package backend.xxx.chat.realtime.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.conversation.service.ConversationResponseBuilder;
import backend.xxx.chat.message.dto.MessageResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageStatus;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.message.repository.MessagePinRepository;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.message.service.MessageMapper;
import backend.xxx.chat.message.service.MessagePinMapper;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageRealtimeNotifierTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessagePinRepository messagePinRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MessagePinMapper messagePinMapper;

    @Mock
    private ConversationResponseBuilder conversationResponseBuilder;

    @Mock
    private ConversationAccessPolicy conversationAccessPolicy;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private MessageRealtimeNotifier notifier;

    @Test
    void notifyCreatedSendsMessageOnlyToActiveParticipants() {
        User alice = user(1L, "alice");
        User bob = user(2L, "bob");
        User carol = user(3L, "carol");
        Conversation conversation = conversation(10L);
        Message message = message(conversation, alice, 100L);
        ConversationParticipant aliceParticipant = ConversationParticipant.create(conversation, alice, true);
        ConversationParticipant bobParticipant = ConversationParticipant.create(conversation, bob, true);
        ConversationParticipant leftParticipant = ConversationParticipant.create(conversation, carol, false);
        leftParticipant.markLeft(Instant.parse("2026-01-01T00:00:00Z"));
        leftParticipant.hideFromList();
        List<ConversationParticipant> allParticipants = List.of(aliceParticipant, bobParticipant, leftParticipant);
        List<ConversationParticipant> activeParticipants = List.of(aliceParticipant, bobParticipant);

        when(messageRepository.findByIdInWithSender(List.of(message.getId())))
                .thenReturn(List.of(message));
        when(messageMapper.toResponse(message)).thenReturn(messageResponse(message));
        when(participantRepository.findByConversationIdWithUser(conversation.getId()))
                .thenReturn(allParticipants);
        when(conversationAccessPolicy.filterActiveParticipants(allParticipants))
                .thenReturn(activeParticipants);
        when(conversationResponseBuilder.buildByUsernameForParticipants(activeParticipants))
                .thenReturn(Map.of());

        notifier.notifyCreated(1L, conversation.getId(), message.getId());

        verify(realtimeEventPublisher).sendToUser(
                eq("outbox_1_message.created"),
                eq("alice"),
                eq(RealtimeEventType.MESSAGE_CREATED),
                eq(conversation.getId()),
                any()
        );
        verify(realtimeEventPublisher).sendToUser(
                eq("outbox_1_message.created"),
                eq("bob"),
                eq(RealtimeEventType.MESSAGE_CREATED),
                eq(conversation.getId()),
                any()
        );
        verify(realtimeEventPublisher, never()).sendToUser(
                any(),
                eq("carol"),
                any(),
                any(),
                any()
        );
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
        Conversation conversation = Conversation.createGroupConversation("Team", null, null);
        conversation.setId(id);
        return conversation;
    }

    private Message message(Conversation conversation, User sender, Long id) {
        Message message = Message.createTextMessage(conversation, sender, UUID.randomUUID(), "hello");
        message.setId(id);
        return message;
    }

    private MessageResponse messageResponse(Message message) {
        User sender = message.getSender();
        return new MessageResponse(
                message.getId(),
                message.getClientMessageId().toString(),
                message.getConversation().getId(),
                new SummarizeUserResponse(
                        sender.getId(),
                        sender.getUsername(),
                        sender.getDisplayName(),
                        sender.getAvatarUrl()
                ),
                message.getContent(),
                null,
                List.of(),
                MessageType.TEXT,
                MessageStatus.SENT,
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                null,
                null,
                null,
                null
        );
    }
}