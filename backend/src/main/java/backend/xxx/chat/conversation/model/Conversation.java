package backend.xxx.chat.conversation.model;

import java.time.Instant;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public static Conversation createDirectConversation() {
        Conversation conversation = new Conversation();
        conversation.type = ConversationType.DIRECT;
        return conversation;
    }

    public static Conversation createGroupConversation(String name, String avatarUrl, User createdBy) {
        Conversation conversation = new Conversation();
        conversation.type = ConversationType.GROUP;
        conversation.name = name;
        conversation.avatarUrl = avatarUrl;
        conversation.createdBy = createdBy;
        return conversation;
    }

    public void updateProfile(String name, String avatarUrl) {
        this.name = name;
        this.avatarUrl = avatarUrl;
    }

    public void updateLastMessage(Long lastMessageId, Instant lastMessageAt) {
        this.lastMessageId = lastMessageId;
        this.lastMessageAt = lastMessageAt;
    }
}
