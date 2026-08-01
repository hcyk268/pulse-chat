package backend.xxx.chat.auth.service;

import java.util.List;

import backend.xxx.chat.auth.model.AuthenticatedUser;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("user.not.found: " + username));

        return toUserDetails(user);
    }

    public UserDetails toUserDetails(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getAccountStatus(),
                user.isEmailVerified(),
                user.getCredentialsVersion(),
                List.of(new SimpleGrantedAuthority(DEFAULT_ROLE))
        );
    }
}
