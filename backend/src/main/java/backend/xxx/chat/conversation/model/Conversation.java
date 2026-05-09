package backend.xxx.chat.conversation.model;

import java.time.Instant;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conversations")
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends AbstractBaseEntity<Long> {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ConversationType type = ConversationType.DIRECT;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public static Conversation createDirectConversation() {
        Conversation conversation = new Conversation();
        conversation.type = ConversationType.DIRECT;
        return conversation;
    }

    public void updateLastMessage(Long lastMessageId, Instant lastMessageAt) {
        this.lastMessageId = lastMessageId;
        this.lastMessageAt = lastMessageAt;
    }
}
