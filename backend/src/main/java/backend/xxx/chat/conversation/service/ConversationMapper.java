package backend.xxx.chat.conversation.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.conversation.dto.ConversationDetailResponse;
import backend.xxx.chat.conversation.dto.ConversationLastMessageResponse;
import backend.xxx.chat.conversation.dto.ConversationMemberResponse;
import backend.xxx.chat.conversation.dto.ConversationParticipantResponse;
import backend.xxx.chat.conversation.dto.ConversationResponse;
import backend.xxx.chat.conversation.dto.ConversationUserResponse;
import backend.xxx.chat.conversation.dto.DirectConversationResponse;
import backend.xxx.chat.conversation.model.Conversation;
import backend.xxx.chat.conversation.model.ConversationType;
import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageType;
import backend.xxx.chat.user.dto.PresenceResponse;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
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
                .orElseThrow(() -> new NotFoundException("conversation.participant.current.not.found"));

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

    public ConversationDetailResponse toConversationDetailResponse(
            Conversation conversation,
            User currentUser,
            List<ConversationParticipant> participants,
            Map<Long, Presence> presenceByUserId,
            Message lastMessage
    ) {
        ConversationParticipant currentParticipant = participants.stream()
                .filter(participant -> participant.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("conversation.participant.current.not.found"));

        List<ConversationParticipant> activeParticipants = participants.stream()
                .filter(ConversationParticipant::isActive)
                .toList();
        List<ConversationParticipant> visibleParticipants = participants.stream()
                .filter(participant -> !participant.isLeft())
                .toList();

        ConversationParticipant peerParticipant = activeParticipants.stream()
                .filter(participant -> !participant.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);

        boolean group = conversation.getType() == ConversationType.GROUP;
        String title = group
                ? conversation.getName()
                : peerParticipant == null ? null : peerParticipant.getUser().getDisplayName();
        String avatarUrl = group
                ? conversation.getAvatarUrl()
                : peerParticipant == null ? null : peerParticipant.getUser().getAvatarUrl();

        ConversationUserResponse peer = group || peerParticipant == null
                ? null
                : toConversationUserResponse(
                        peerParticipant.getUser(),
                        presenceByUserId.get(peerParticipant.getUser().getId())
                );

        List<ConversationMemberResponse> memberResponses = visibleParticipants.stream()
                .sorted(Comparator.comparing(participant -> participant.getUser().getId()))
                .map(participant -> toConversationMemberResponse(
                        participant,
                        presenceByUserId.get(participant.getUser().getId())
                ))
                .toList();

        ConversationUserResponse createdBy = conversation.getCreatedBy() == null
                ? null
                : toConversationUserResponse(
                        conversation.getCreatedBy(),
                        presenceByUserId.get(conversation.getCreatedBy().getId())
                );

        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getType(),
                title,
                avatarUrl,
                peer,
                createdBy,
                group ? currentParticipant.getRole() : null,
                memberResponses,
                activeParticipants.size(),
                toConversationLastMessageResponse(lastMessage),
                currentParticipant.getUnreadCount()
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
                .filter(participant -> !participant.isLeft() && !participant.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);
        Message lastMessage = conversation.getLastMessageId() == null
                ? null
                : lastMessageById.get(conversation.getLastMessageId());

        boolean group = conversation.getType() == ConversationType.GROUP;
        String title = group
                ? conversation.getName()
                : otherParticipant == null ? null : otherParticipant.getUser().getDisplayName();
        String avatarUrl = group
                ? conversation.getAvatarUrl()
                : otherParticipant == null ? null : otherParticipant.getUser().getAvatarUrl();
        ConversationUserResponse peer = group || otherParticipant == null
                ? null
                : toConversationUserResponse(
                        otherParticipant.getUser(),
                        presenceByUserId.get(otherParticipant.getUser().getId())
                );
        int participantCount = (int) participants.stream()
                .filter(ConversationParticipant::isActive)
                .count();

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                title,
                avatarUrl,
                peer,
                group ? currentParticipant.getRole() : null,
                participantCount,
                toConversationLastMessageResponse(lastMessage),
                currentParticipant.getUnreadCount()
        );
    }

    private ConversationMemberResponse toConversationMemberResponse(
            ConversationParticipant participant,
            Presence presence
    ) {
        User user = participant.getUser();
        return new ConversationMemberResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                toPresenceResponse(presence),
                participant.getRole(),
                participant.getJoinedAt(),
                participant.getLeftAt()
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
