package backend.xxx.chat.realtime.service;

import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.realtime.dto.TypingStatusRequest;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.model.TypingUpdatedEventData;
import backend.xxx.chat.user.service.CachedUser;
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

        CachedUser currentUser = userLookupService.getCurrentUserCached(currentUsername);
        conversationAccessPolicy.assertCanUpdateTyping(conversationId, currentUser.id());

        TypingUpdatedEventData data = new TypingUpdatedEventData(
                currentUser.id(),
                currentUser.username(),
                request.typing()
        );

        conversationAccessPolicy.requireParticipantSnapshots(conversationId).stream()
                .filter(participant -> !participant.userId().equals(currentUser.id()))
                .map(participant -> participant.username())
                .forEach(username -> realtimeEventPublisher.sendToUser(
                        username,
                        RealtimeEventType.TYPING_UPDATED,
                        conversationId,
                        data
                ));
    }
}