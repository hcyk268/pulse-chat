package backend.xxx.chat.message.service;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.conversation.service.ConversationAccessPolicy;
import backend.xxx.chat.message.dto.MessageReactionRequest;
import backend.xxx.chat.message.dto.MessageReactionResponse;
import backend.xxx.chat.message.dto.MessageReactionsResponse;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.message.model.MessageReaction;
import backend.xxx.chat.message.model.MessageReactionEmoji;
import backend.xxx.chat.message.repository.MessageReactionRepository;
import backend.xxx.chat.message.repository.MessageRepository;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageReactionService {

    private final UserLookupService userLookupService;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final ConversationAccessPolicy conversationAccessPolicy;
    private final MessageReactionMapper messageReactionMapper;

    @Transactional
    public ReactMessageResult reactMessage(
            String currentUsername,
            Long messageId,
            MessageReactionRequest request
    ) {
        validateReactionRequest(messageId, request);

        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Message message = getMessage(messageId);
        conversationAccessPolicy.requireParticipant(message.getConversation().getId(), currentUser.getId());

        if (message.isDeleted()) {
            throw new ValidationException("Deleted message cannot be reacted");
        }

        MessageReaction existingReaction = messageReactionRepository
                .findByMessageIdAndUserIdAndEmojiWithDetails(messageId, currentUser.getId(), request.emoji())
                .orElse(null);
        if (existingReaction != null) {
            return new ReactMessageResult(messageReactionMapper.toResponse(existingReaction), false);
        }

        MessageReaction savedReaction = messageReactionRepository.saveAndFlush(
                MessageReaction.create(message, currentUser, request.emoji())
        );

        return new ReactMessageResult(messageReactionMapper.toResponse(savedReaction), true);
    }

    @Transactional
    public void removeReaction(
            String currentUsername,
            Long messageId,
            MessageReactionEmoji emoji
    ) {
        validateRemoveReactionRequest(messageId, emoji);

        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Message message = getMessage(messageId);
        conversationAccessPolicy.requireParticipant(message.getConversation().getId(), currentUser.getId());

        messageReactionRepository
                .findByMessageIdAndUserIdAndEmojiWithDetails(messageId, currentUser.getId(), emoji)
                .ifPresent(messageReactionRepository::delete);
    }

    @Transactional(readOnly = true)
    public MessageReactionsResponse getReactions(String currentUsername, Long messageId) {
        if (messageId == null) {
            throw new ValidationException("messageId must not be null");
        }

        User currentUser = userLookupService.getCurrentUser(currentUsername);
        Message message = getMessage(messageId);
        conversationAccessPolicy.requireParticipant(message.getConversation().getId(), currentUser.getId());

        return messageReactionMapper.toReactionsResponse(
                messageId,
                messageReactionRepository.findByMessageIdWithUser(messageId),
                currentUser.getId()
        );
    }

    private Message getMessage(Long messageId) {
        return messageRepository.findByIdWithConversationAndSender(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
    }

    private void validateReactionRequest(Long messageId, MessageReactionRequest request) {
        if (messageId == null) {
            throw new ValidationException("messageId must not be null");
        }

        if (request == null || request.emoji() == null) {
            throw new ValidationException("emoji must not be null");
        }
    }

    private void validateRemoveReactionRequest(Long messageId, MessageReactionEmoji emoji) {
        if (messageId == null) {
            throw new ValidationException("messageId must not be null");
        }

        if (emoji == null) {
            throw new ValidationException("emoji must not be null");
        }
    }

    public record ReactMessageResult(
            MessageReactionResponse response,
            boolean created
    ) {
    }
}
