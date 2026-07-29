package backend.xxx.chat.auth.model;

import java.util.Collection;

import backend.xxx.chat.user.model.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Setter
@Getter
@AllArgsConstructor
public class AuthenticatedUser implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private AccountStatus accountStatus;
    private boolean emailVerified;
    private Long credentialsVersion;
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.SUSPENDED
                && accountStatus != AccountStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus != AccountStatus.INACTIVE;
    }

    public boolean canUseTokens() {
        return isEnabled() && isAccountNonLocked() && emailVerified;
    }
}
