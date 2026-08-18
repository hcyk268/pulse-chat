package backend.xxx.chat.ai.prompt;

import backend.xxx.chat.ai.config.AiDefaults;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import backend.xxx.chat.ai.client.AiChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptBudgeter {

    private final ObjectMapper objectMapper;

    public int maxInputChars() {
        return AiDefaults.MAX_INPUT_CHARS;
    }

    public String fitInput(String input) {
        return fitJson(input, AiDefaults.MAX_INPUT_CHARS);
    }

    public String fitJson(String input, int maxChars) {
        if (input == null || input.length() <= maxChars) {
            return input;
        }
        int payloadBudget = Math.max(0, maxChars - 160);
        while (payloadBudget >= 0) {
            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.put("truncated", true);
            wrapper.put("notice", "Original JSON exceeded the input budget; prefix and suffix are JSON strings.");
            wrapper.put("originalJsonPrefix", safePrefix(input, payloadBudget / 2));
            wrapper.put("originalJsonSuffix", safeSuffix(input, payloadBudget - payloadBudget / 2));
            try {
                String serialized = objectMapper.writeValueAsString(wrapper);
                if (serialized.length() <= maxChars) {
                    return serialized;
                }
                payloadBudget -= Math.max(16, serialized.length() - maxChars + 8);
            } catch (JsonProcessingException ex) {
                return "{\"truncated\":true}";
            }
            if (payloadBudget == 0) {
                return "{\"truncated\":true}";
            }
        }
        return "{\"truncated\":true}";
    }

    public List<AiChatMessage> fitMessages(List<AiChatMessage> messages, int maxChars) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int totalChars = messages.stream()
                .mapToInt(message -> message.content() == null ? 0 : message.content().length())
                .sum();
        if (totalChars <= maxChars) {
            return List.copyOf(messages);
        }
        int anchorBudget = Math.max(256, maxChars / 4);
        List<AiChatMessage> result = new ArrayList<>();
        result.add(boundedMessage(messages.get(0), anchorBudget));
        int usedChars = result.get(0).content().length();
        if (messages.size() > 1) {
            result.add(boundedMessage(messages.get(1), anchorBudget));
            usedChars += result.get(1).content().length();
        }
        List<AiChatMessage> recent = new ArrayList<>();
        for (int index = messages.size() - 1; index >= 2; index--) {
            int remaining = maxChars - usedChars;
            if (remaining <= 64) {
                break;
            }
            String content = messages.get(index).content() == null ? "" : messages.get(index).content();
            String fitted = truncateTail(content, remaining);
            recent.add(new AiChatMessage(messages.get(index).role(), fitted));
            usedChars += fitted.length();
            if (fitted.length() < content.length()) {
                break;
            }
        }
        Collections.reverse(recent);
        result.addAll(recent);
        return List.copyOf(result);
    }

    private AiChatMessage boundedMessage(AiChatMessage message, int maxChars) {
        return new AiChatMessage(message.role(), truncateTail(message.content() == null ? "" : message.content(), maxChars));
    }

    private String safePrefix(String input, int length) {
        int end = Math.min(input.length(), Math.max(0, length));
        if (end > 0 && end < input.length() && Character.isHighSurrogate(input.charAt(end - 1))) {
            end--;
        }
        return input.substring(0, end);
    }

    private String safeSuffix(String input, int length) {
        int start = Math.max(0, input.length() - Math.max(0, length));
        if (start > 0 && start < input.length() && Character.isLowSurrogate(input.charAt(start))) {
            start++;
        }
        return input.substring(start);
    }

    public String truncate(String input, int maxChars) {
        return truncateMiddle(input, maxChars);
    }

    public String truncateTail(String input, int maxChars) {
        if (input == null || input.length() <= maxChars) {
            return input;
        }
        String marker = " [TRUNCATED]";
        int contentLength = Math.max(0, maxChars - marker.length());
        return safePrefix(input, contentLength).trim() + marker;
    }

    private String truncateMiddle(String input, int maxChars) {
        if (input == null || input.length() <= maxChars) {
            return input;
        }
        int markerLength = "\n[TRUNCATED]\n".length();
        int remaining = Math.max(0, maxChars - markerLength);
        int head = remaining / 2;
        int tail = remaining - head;
        return safePrefix(input, head).trim()
                + "\n[TRUNCATED]\n"
                + safeSuffix(input, tail).trim();
    }
}