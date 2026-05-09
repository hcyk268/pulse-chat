package backend.xxx.chat.conversation.repository;

import java.util.Collection;
import java.util.List;

import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.conversation.model.ConversationParticipantId;
import backend.xxx.chat.conversation.model.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

    @Query("""
            select targetParticipant.user.id as userId,
                max(targetParticipant.conversation.id) as conversationId
            from ConversationParticipant currentParticipant, ConversationParticipant targetParticipant
            where currentParticipant.conversation = targetParticipant.conversation
                and currentParticipant.user.id = :currentUserId
                and targetParticipant.user.id in :targetUserIds
                and currentParticipant.conversation.type = :conversationType
                and targetParticipant.user.id <> :currentUserId
            group by targetParticipant.user.id
            """)
    List<DirectConversationLookup> findDirectConversationIds(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserIds") Collection<Long> targetUserIds,
            @Param("conversationType") ConversationType conversationType
    );

    interface DirectConversationLookup {
        Long getUserId();

        Long getConversationId();
    }
}
