package backend.xxx.chat.conversation.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConversationParticipantId implements Serializable {

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
