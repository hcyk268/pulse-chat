package backend.xxx.chat.conversation.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import backend.xxx.chat.conversation.dto.ConversationLastMessageResponse;
import backend.xxx.chat.conversation.dto.ConversationParticipantResponse;
import backend.xxx.chat.conversation.dto.ConversationResponse;
import backend.xxx.chat.conversation.dto.ConversationUserResponse;
import backend.xxx.chat.conversation.dto.DirectConversationResponse;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.user.dto.PresenceResponse;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    private static final int LAST_MESSAGE_PREVIEW_LENGTH = 120;

    public DirectConversationResponse toDirectConversationResponse(
            Conversation conversation,
            List<ConversationParticipant> participants,
            User currentUser,
            User targetUser,
            Map<Long, Presence> presenceByUserId,
            Message lastMessage
    ) {
        ConversationParticipant currentParticipant = participants.stream()
                .filter(participant -> participant.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Current participant not found"
                ));

        List<ConversationParticipantResponse> participantResponses = participants.stream()
                .sorted(Comparator.comparing(participant -> participant.getUser().getId()))
                .map(participant -> toParticipantResponse(
                        participant,
                        presenceByUserId.get(participant.getUser().getId())
                ))
                .toList();

        return new DirectConversationResponse(
                conversation.getId(),
                conversation.getType(),
                participantResponses,
                toConversationUserResponse(targetUser, presenceByUserId.get(targetUser.getId())),
                currentParticipant.getUnreadCount(),
                toConversationLastMessageResponse(lastMessage),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public ConversationResponse toConversationResponse(
            ConversationParticipant currentParticipant,
            User currentUser,
            List<ConversationParticipant> participants,
            Map<Long, Presence> presenceByUserId,
            Map<Long, Message> lastMessageById
    ) {
        Conversation conversation = currentParticipant.getConversation();
        ConversationParticipant otherParticipant = participants.stream()
                .filter(participant -> !participant.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);
        Message lastMessage = conversation.getLastMessageId() == null
                ? null
                : lastMessageById.get(conversation.getLastMessageId());

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                otherParticipant == null
                        ? null
                        : toConversationUserResponse(
                                otherParticipant.getUser(),
                                presenceByUserId.get(otherParticipant.getUser().getId())
                        ),
                currentParticipant.getUnreadCount(),
                toConversationLastMessageResponse(lastMessage),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private ConversationParticipantResponse toParticipantResponse(
            ConversationParticipant participant,
            Presence presence
    ) {
        User user = participant.getUser();
        return new ConversationParticipantResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                toPresenceResponse(presence),
                participant.isVisibleInList()
        );
    }

    private ConversationUserResponse toConversationUserResponse(User user, Presence presence) {
        return new ConversationUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                toPresenceResponse(presence)
        );
    }

    private ConversationLastMessageResponse toConversationLastMessageResponse(Message message) {
        if (message == null) {
            return null;
        }

        return new ConversationLastMessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.isDeleted() ? null : toContentPreview(message),
                message.getStatus(),
                message.getCreatedAt(),
                message.getDeletedAt()
        );
    }

    private String toContentPreview(Message message) {
        String content = message.getContent();
        if (content != null && !content.isBlank()) {
            return trimPreview(content);
        }

        if (message.getMessageType() == MessageType.MEDIA && !message.getAttachments().isEmpty()) {
            if (message.getAttachments().size() == 1) {
                return trimPreview(message.getAttachments().get(0).getFileName());
            }

            return message.getAttachments().size() + " attachments";
        }

        return null;
    }

    private String trimPreview(String content) {
        if (content.length() <= LAST_MESSAGE_PREVIEW_LENGTH) {
            return content;
        }

        return content.substring(0, LAST_MESSAGE_PREVIEW_LENGTH - 3) + "...";
    }

    private PresenceResponse toPresenceResponse(Presence presence) {
        return new PresenceResponse(
                presence != null && presence.isOnline(),
                presence == null ? null : presence.getLastActiveAt()
        );
    }
}
