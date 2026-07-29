package backend.xxx.chat.auth.dto;

public record RegisterResponse(
        String email,
        boolean verificationRequired,
        long verificationTokenExpiresInMs
) {
}
