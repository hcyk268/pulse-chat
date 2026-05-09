package backend.xxx.chat.user.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import backend.xxx.chat.common.dto.CursorPageResponse;
import backend.xxx.chat.common.exception.UnauthorizedException;
import backend.xxx.chat.conversation.model.ConversationType;
import backend.xxx.chat.conversation.repository.ConversationParticipantRepository;
import backend.xxx.chat.user.dto.UpdateMyProfileRequest;
import backend.xxx.chat.user.dto.UserResponse;
import backend.xxx.chat.user.dto.UserSearchItemResponse;
import backend.xxx.chat.user.dto.UserSearchResponse;
import backend.xxx.chat.user.model.AccountStatus;
import backend.xxx.chat.user.model.Presence;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.PresenceRepository;
import backend.xxx.chat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int MAX_SEARCH_LIMIT = 100;
    private static final int MAX_SEARCH_KEYWORD_LENGTH = 100;

    private final UserRepository userRepository;
    private final PresenceRepository presenceRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String username) {
        User user = findCurrentUser(username);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateMyProfile(String username, UpdateMyProfileRequest request) {
        User user = findCurrentUser(username);
        user.updateProfile(request.displayName(), request.avatarUrl(), request.bio());
        return userMapper.toResponse(user);
    }

    private User findCurrentUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UnauthorizedException("Current user not found"));
    }

    @Transactional(readOnly = true)
    public UserSearchResponse search(String currentUsername, String keyword, Short limit) {
        User currentUser = findCurrentUser(currentUsername);
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        int normalizedLimit = normalizeSearchLimit(limit);

        List<User> users = userRepository.searchActiveUsers(
                currentUser.getId(),
                normalizedKeyword,
                AccountStatus.ACTIVE,
                PageRequest.of(0, normalizedLimit + 1)
        );

        boolean hasMore = users.size() > normalizedLimit;
        List<User> pageUsers = hasMore ? users.subList(0, normalizedLimit) : users;
        Map<Long, Presence> presenceByUserId = findPresenceByUserId(pageUsers);
        Map<Long, Long> directConversationIdByUserId = findDirectConversationIdByUserId(
                currentUser.getId(),
                pageUsers
        );

        List<UserSearchItemResponse> items = pageUsers.stream()
                .map(user -> userMapper.toSearchItem(
                        user,
                        presenceByUserId.get(user.getId()),
                        directConversationIdByUserId.get(user.getId())
                ))
                .toList();

        return new UserSearchResponse(
                items,
                new CursorPageResponse(normalizedLimit, null, hasMore)
        );
    }

    private Map<Long, Presence> findPresenceByUserId(List<User> users) {
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return presenceRepository.findByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(Presence::getUserId, Function.identity()));
    }

    private Map<Long, Long> findDirectConversationIdByUserId(Long currentUserId, List<User> users) {
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return conversationParticipantRepository.findDirectConversationIds(
                        currentUserId,
                        userIds,
                        ConversationType.DIRECT
                )
                .stream()
                .collect(Collectors.toMap(
                        ConversationParticipantRepository.DirectConversationLookup::getUserId,
                        ConversationParticipantRepository.DirectConversationLookup::getConversationId
                ));
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("q must not be blank");
        }

        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.length() > MAX_SEARCH_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("q exceeds max length " + MAX_SEARCH_KEYWORD_LENGTH);
        }

        return normalizedKeyword;
    }

    private int normalizeSearchLimit(Short limit) {
        if (limit == null) {
            return DEFAULT_SEARCH_LIMIT;
        }

        if (limit < 1 || limit > MAX_SEARCH_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_SEARCH_LIMIT);
        }

        return limit;
    }
}
