package backend.xxx.chat.message.model;

import java.util.Objects;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(
        name = "message_reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_message_reactions_message_user_emoji",
                        columnNames = {"message_id", "user_id", "emoji"}
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class MessageReaction extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "emoji", nullable = false, length = 32)
    private MessageReactionEmoji emoji;

    public static MessageReaction create(
            Message message,
            User user,
            MessageReactionEmoji emoji
    ) {
        MessageReaction reaction = new MessageReaction();
        reaction.message = Objects.requireNonNull(message, "message must not be null");
        reaction.user = Objects.requireNonNull(user, "user must not be null");
        reaction.emoji = Objects.requireNonNull(emoji, "message.reaction.emoji.required");
        return reaction;
    }
}
