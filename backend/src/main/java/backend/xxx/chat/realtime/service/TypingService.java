package backend.xxx.chat.realtime.service;

import java.util.List;

import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.realtime.dto.TypingStatusRequest;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.model.TypingUpdatedEventData;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TypingService {

    private final UserLookupService userLookupService;
    private final ConversationAccessPolicy conversationAccessPolicy;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final RealtimeValidator realtimeValidator;

    @Transactional(readOnly = true)
    public void updateTyping(String currentUsername, Long conversationId, TypingStatusRequest request) {
        realtimeValidator.validateTypingRequest(conversationId, request);

        User currentUser = userLookupService.getCurrentUser(currentUsername);

        List<ConversationParticipant> participants =
                conversationAccessPolicy.requireParticipants(conversationId);
        conversationAccessPolicy.assertCanUpdateTyping(currentUser, participants);

        TypingUpdatedEventData data = new TypingUpdatedEventData(
                currentUser.getId(),
                currentUser.getUsername(),
                request.typing()
        );

        participants.stream()
                .filter(participant -> !participant.getUser().getId().equals(currentUser.getId()))
                .map(participant -> participant.getUser().getUsername())
                .forEach(username -> realtimeEventPublisher.sendToUser(
                        username,
                        RealtimeEventType.TYPING_UPDATED,
                        conversationId,
                        data
                ));
    }
}
