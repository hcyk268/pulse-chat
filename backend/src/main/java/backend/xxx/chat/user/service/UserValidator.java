package backend.xxx.chat.user.service;

import org.springframework.stereotype.Component;

@Component
public class UserValidator {

    public String normalizeSearchKeyword(String keyword, int maxLength) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("q must not be blank");
        }

        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.length() > maxLength) {
            throw new IllegalArgumentException("q exceeds max length " + maxLength);
        }

        return normalizedKeyword;
    }

    public int normalizeSearchLimit(Short limit, int defaultLimit, int maxLimit) {
        if (limit == null) {
            return defaultLimit;
        }

        if (limit < 1 || limit > maxLimit) {
            throw new IllegalArgumentException("limit must be between 1 and " + maxLimit);
        }

        return limit;
    }
}
