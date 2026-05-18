package backend.xxx.chat.message.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import backend.xxx.chat.message.dto.MessageReactionGroupResponse;
import backend.xxx.chat.message.dto.MessageReactionResponse;
import backend.xxx.chat.message.dto.MessageReactionsResponse;
import backend.xxx.chat.message.model.MessageReaction;
import backend.xxx.chat.message.model.MessageReactionEmoji;
import backend.xxx.chat.user.dto.SummarizeUserResponse;
import backend.xxx.chat.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class MessageReactionMapper {

    public MessageReactionResponse toResponse(MessageReaction reaction) {
        return new MessageReactionResponse(
                reaction.getMessage().getId(),
                reaction.getEmoji(),
                toUserResponse(reaction.getUser()),
                reaction.getCreatedAt()
        );
    }

    public MessageReactionsResponse toReactionsResponse(
            Long messageId,
            List<MessageReaction> reactions,
            Long currentUserId
    ) {
        Map<MessageReactionEmoji, List<MessageReaction>> reactionsByEmoji =
                new EnumMap<>(MessageReactionEmoji.class);

        reactions.forEach(reaction -> reactionsByEmoji
                .computeIfAbsent(reaction.getEmoji(), ignored -> new ArrayList<>())
                .add(reaction));

        List<MessageReactionGroupResponse> items = reactionsByEmoji.entrySet()
                .stream()
                .map(entry -> toGroupResponse(entry.getKey(), entry.getValue(), currentUserId))
                .toList();

        return new MessageReactionsResponse(messageId, items);
    }

    private MessageReactionGroupResponse toGroupResponse(
            MessageReactionEmoji emoji,
            List<MessageReaction> reactions,
            Long currentUserId
    ) {
        boolean reactedByMe = reactions.stream()
                .anyMatch(reaction -> reaction.getUser().getId().equals(currentUserId));
        List<SummarizeUserResponse> users = reactions.stream()
                .map(MessageReaction::getUser)
                .map(this::toUserResponse)
                .toList();

        return new MessageReactionGroupResponse(
                emoji,
                reactions.size(),
                reactedByMe,
                users
        );
    }

    private SummarizeUserResponse toUserResponse(User user) {
        return new SummarizeUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }
}
