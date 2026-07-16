package backend.xxx.chat.user.service;

import backend.xxx.chat.common.exception.UnauthorizedException;
import backend.xxx.chat.common.exception.UserNotFoundException;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLookupService {

    private final UserRepository userRepository;

    public User getCurrentUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UnauthorizedException("user.current.not.found"));
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}
