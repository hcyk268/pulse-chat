package backend.xxx.chat.ai.safety;

import backend.xxx.chat.common.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class AiResponseValidator {

    public String requiredPlainText(String content, int maxLength) {
        if (content == null || content.trim().isBlank()) {
            throw new ValidationException("ai.response.empty");
        }
        String value = stripCodeFence(content.trim());
        return value.length() <= maxLength ? value : value.substring(0, maxLength).trim();
    }

    private String stripCodeFence(String value) {
        if (!value.startsWith("```")) {
            return value;
        }
        String withoutOpeningFence = value.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return withoutOpeningFence.replaceFirst("\\s*```$", "");
    }
}