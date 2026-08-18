package backend.xxx.chat.ai.prompt;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class AiLanguagePolicy {

    public static final String DEFAULT_LOCALE = "en";
    public static final String VIETNAMESE_LOCALE = "vi";

    public String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        Locale parsed = Locale.forLanguageTag(locale.trim().replace('_', '-'));
        return VIETNAMESE_LOCALE.equalsIgnoreCase(parsed.getLanguage())
                ? VIETNAMESE_LOCALE
                : DEFAULT_LOCALE;
    }

    public String responseLanguageInstruction(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        String language = VIETNAMESE_LOCALE.equals(normalizedLocale) ? "Vietnamese" : "English";
        return "Response locale: " + normalizedLocale + ". "
                + "Write every user-facing natural-language string value in " + language + ", "
                + "even when the input data or user question is written in another language. "
                + "If the user explicitly requests translation or a different output language, follow that explicit request. "
                + "Keep JSON keys, enum values, identifiers, symbols, category/tag slugs, tool names, "
                + "proper nouns, and quoted source text unchanged.";
    }
}
