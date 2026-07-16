package backend.xxx.chat.user.model;

import java.util.Objects;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import backend.xxx.chat.common.web.Translator;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class User extends AbstractBaseEntity<Long> {

    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int PASSWORD_HASH_MAX_LENGTH = 255;
    private static final int DISPLAY_NAME_MAX_LENGTH = 100;
    private static final int AVATAR_URL_MAX_LENGTH = 500;
    private static final int BIO_MAX_LENGTH = 500;

    @Column(name = "username", nullable = false, length = USERNAME_MAX_LENGTH)
    private String username;

    @Column(name = "email", nullable = false, length = EMAIL_MAX_LENGTH)
    private String email;

    @Column(name = "password_hash", nullable = false, length = PASSWORD_HASH_MAX_LENGTH)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = DISPLAY_NAME_MAX_LENGTH)
    private String displayName;

    @Column(name = "avatar_url", length = AVATAR_URL_MAX_LENGTH)
    private String avatarUrl;

    @Column(name = "bio", length = BIO_MAX_LENGTH)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    public static User create(String username, String email, String passwordHash, String displayName) {
        User user = new User();
        user.username = requireText(username, "username", USERNAME_MAX_LENGTH);
        user.email = requireText(email, "email", EMAIL_MAX_LENGTH);
        user.passwordHash = requireText(passwordHash, "passwordHash", PASSWORD_HASH_MAX_LENGTH);
        user.displayName = requireText(displayName, "displayName", DISPLAY_NAME_MAX_LENGTH);
        user.accountStatus = AccountStatus.ACTIVE;
        return user;
    }

    public void updateProfile(String displayName, String avatarUrl, String bio) {
        if (displayName != null) {
            this.displayName = requireText(displayName, "displayName", DISPLAY_NAME_MAX_LENGTH);
        }

        if (avatarUrl != null) {
            this.avatarUrl = normalizeOptionalText(avatarUrl, "avatarUrl", AVATAR_URL_MAX_LENGTH);
        }

        if (bio != null) {
            this.bio = normalizeOptionalText(bio, "bio", BIO_MAX_LENGTH);
        }
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = requireText(passwordHash, "passwordHash", PASSWORD_HASH_MAX_LENGTH);
    }

    public void changeEmail(String email) {
        ensureNotBanned();
        this.email = requireText(email, "email", EMAIL_MAX_LENGTH);
    }

    public void activate() {
        this.accountStatus = AccountStatus.ACTIVE;
    }

    public void deactivate() {
        ensureNotBanned();
        this.accountStatus = AccountStatus.INACTIVE;
    }

    public void suspend() {
        ensureNotBanned();
        this.accountStatus = AccountStatus.SUSPENDED;
    }

    public void ban() {
        this.accountStatus = AccountStatus.BANNED;
    }

    public boolean isActive() {
        return this.accountStatus == AccountStatus.ACTIVE;
    }

    public boolean canAuthenticate() {
        return this.accountStatus == AccountStatus.ACTIVE;
    }

    public boolean isLocked() {
        return this.accountStatus == AccountStatus.SUSPENDED || this.accountStatus == AccountStatus.BANNED;
    }

    private void ensureNotBanned() {
        if (this.accountStatus == AccountStatus.BANNED) {
            throw new IllegalStateException("Banned user cannot be changed");
        }
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmedValue = value.trim();

        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException(Translator.toLocale("validation.field.blank", fieldName));
        }

        if (trimmedValue.length() > maxLength) {
            throw new IllegalArgumentException(Translator.toLocale("validation.field.max.length", fieldName, maxLength));
        }

        return trimmedValue;
    }

    private static String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        if (trimmedValue.length() > maxLength) {
            throw new IllegalArgumentException(Translator.toLocale("validation.field.max.length", fieldName, maxLength));
        }

        return trimmedValue;
    }
}
