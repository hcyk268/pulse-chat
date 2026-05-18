package backend.xxx.chat.message.repository;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.message.model.MessageReaction;
import backend.xxx.chat.message.model.MessageReactionEmoji;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    @Query("""
            from MessageReaction reaction
            join fetch reaction.message message
            join fetch reaction.user
            where message.id = :messageId
                and reaction.user.id = :userId
                and reaction.emoji = :emoji
            """)
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmojiWithDetails(
            @Param("messageId") Long messageId,
            @Param("userId") Long userId,
            @Param("emoji") MessageReactionEmoji emoji
    );

    @Query("""
            from MessageReaction reaction
            join fetch reaction.user
            where reaction.message.id = :messageId
            order by reaction.emoji asc, reaction.createdAt asc, reaction.id asc
            """)
    List<MessageReaction> findByMessageIdWithUser(@Param("messageId") Long messageId);
}
