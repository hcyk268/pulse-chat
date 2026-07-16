package backend.xxx.chat.user.service;

import org.springframework.stereotype.Component;
import backend.xxx.chat.common.web.Translator;

@Component
public class UserValidator {

    public String normalizeSearchKeyword(String keyword, int maxLength) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("user.search.query.blank");
        }

        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.length() > maxLength) {
            throw new IllegalArgumentException(Translator.toLocale("user.search.query.max.length", maxLength));
        }

        return normalizedKeyword;
    }

    public int normalizeSearchLimit(Short limit, int defaultLimit, int maxLimit) {
        if (limit == null) {
            return defaultLimit;
        }

        if (limit < 1 || limit > maxLimit) {
            throw new IllegalArgumentException(Translator.toLocale("validation.limit.range", maxLimit));
        }

        return limit;
    }
}
