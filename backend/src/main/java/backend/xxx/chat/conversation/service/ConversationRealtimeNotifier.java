package backend.xxx.chat.conversation.service;

import java.util.LinkedHashSet;
import java.util.Set;

import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationRealtimeNotifier {

    private final ConversationParticipantRepository conversationParticipantRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public <T> void notifyParticipantsAfterCommit(
            Long conversationId,
            RealtimeEventType eventType,
            T data
    ) {
        Set<String> usernames = conversationParticipantRepository
                .findByConversationIdWithUser(conversationId)
                .stream()
                .map(participant -> participant.getUser().getUsername())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Runnable notifyParticipants = () -> usernames.forEach(username -> {
            try {
                realtimeEventPublisher.sendToUser(username, eventType, conversationId, data);
            } catch (RuntimeException publishError) {
                log.warn(
                        "Could not publish {} for conversation {} to {}",
                        eventType,
                        conversationId,
                        username,
                        publishError
                );
            }
        });

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notifyParticipants.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifyParticipants.run();
            }
        });
    }
}
