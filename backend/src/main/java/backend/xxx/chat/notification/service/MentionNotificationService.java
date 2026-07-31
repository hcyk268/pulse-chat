package backend.xxx.chat.notification.service;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import backend.xxx.chat.conversation.model.ConversationParticipant;
import backend.xxx.chat.message.model.Message;
import backend.xxx.chat.notification.model.NotificationTargetType;
import backend.xxx.chat.notification.model.NotificationType;
import backend.xxx.chat.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MentionNotificationService {

    private static final Pattern MENTION_PATTERN =
            Pattern.compile("(?<![A-Za-z0-9._-])@([A-Za-z0-9_-](?:[A-Za-z0-9._-]{0,48}[A-Za-z0-9_-])?)");

    private final NotificationService notificationService;

    public void notifyMentions(
            Message message,
            User sender,
            List<ConversationParticipant> participants
    ) {
        Set<String> mentionedUsernames = extractUsernames(message.getContent());
        if (mentionedUsernames.isEmpty()) {
            return;
        }

        Map<String, User> activeParticipants = participants.stream()
                .filter(ConversationParticipant::isActive)
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(sender.getId()))
                .collect(Collectors.toMap(
                        user -> user.getUsername().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (first, second) -> first
                ));

        mentionedUsernames.stream()
                .map(activeParticipants::get)
                .filter(Objects::nonNull)
                .forEach(recipient -> notificationService.create(new NotificationCommand(
                        recipient,
                        sender,
                        NotificationType.MENTION,
                        "You were mentioned",
                        sender.getDisplayName() + " mentioned you in a message.",
                        NotificationTargetType.MESSAGE,
                        message.getId(),
                        "MESSAGE",
                        message.getId(),
                        "message-mention:" + message.getId() + ":" + recipient.getId()
                )));
    }

    Set<String> extractUsernames(String content) {
        if (content == null || content.isBlank()) {
            return Set.of();
        }

        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return usernames;
    }
}
