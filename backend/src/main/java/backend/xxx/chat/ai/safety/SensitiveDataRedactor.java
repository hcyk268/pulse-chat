package backend.xxx.chat.ai.safety;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SensitiveDataRedactor {

    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d .-]{7,}\\d)(?!\\d)");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern PRIVATE_KEY = Pattern.compile(
            "(?s)-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----.*?-----END [A-Z0-9 ]*PRIVATE KEY-----"
    );
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)\\b(api[_-]?key|secret|token|password|authorization|refresh[_-]?token)\\b\\s*[:=]\\s*['\\\"]?[^\\s,'\\\"]+"
    );
    private static final Set<String> EMAIL_FIELD_NAMES = Set.of("email", "emailaddress");
    private static final Set<String> PHONE_FIELD_NAMES = Set.of("phone", "phonenumber", "mobile", "mobilenumber");
    private static final Set<String> SECRET_FIELD_NAMES = Set.of(
            "apikey",
            "api_key",
            "secret",
            "token",
            "password",
            "authorization",
            "refreshtoken",
            "refresh_token",
            "accesstoken",
            "access_token",
            "idtoken",
            "id_token"
    );

    private final ObjectMapper objectMapper;

    public String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String redacted = EMAIL.matcher(value).replaceAll("[REDACTED_EMAIL]");
        redacted = JWT.matcher(redacted).replaceAll("[REDACTED_JWT]");
        redacted = BEARER_TOKEN.matcher(redacted).replaceAll("Bearer [REDACTED_SECRET]");
        redacted = PRIVATE_KEY.matcher(redacted).replaceAll("[REDACTED_PRIVATE_KEY]");
        redacted = SECRET_ASSIGNMENT.matcher(redacted).replaceAll("$1=[REDACTED_SECRET]");
        redacted = PHONE.matcher(redacted).replaceAll("[REDACTED_PHONE]");
        return redacted;
    }

    public List<String> redactAll(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().map(this::redact).toList();
    }

    public String redactObject(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            JsonNode redacted = redactJsonNode(objectMapper.valueToTree(value), null);
            return objectMapper.writeValueAsString(redacted);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            return redact(String.valueOf(value));
        }
    }

    private JsonNode redactJsonNode(JsonNode node, String fieldName) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode copy = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                copy.set(field.getKey(), redactJsonNode(field.getValue(), field.getKey()));
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = objectMapper.createArrayNode();
            node.forEach(item -> copy.add(redactJsonNode(item, fieldName)));
            return copy;
        }
        if (!node.isTextual()) {
            return node;
        }
        return new TextNode(redactTextField(fieldName, node.asText()));
    }

    private String redactTextField(String fieldName, String value) {
        String normalizedFieldName = normalizeFieldName(fieldName);
        if (EMAIL_FIELD_NAMES.contains(normalizedFieldName)) {
            return "[REDACTED_EMAIL]";
        }
        if (PHONE_FIELD_NAMES.contains(normalizedFieldName)) {
            return "[REDACTED_PHONE]";
        }
        if (SECRET_FIELD_NAMES.contains(normalizedFieldName)) {
            return "[REDACTED_SECRET]";
        }
        return redact(value);
    }

    private String normalizeFieldName(String fieldName) {
        if (fieldName == null) {
            return "";
        }
        return fieldName.replaceAll("[^A-Za-z0-9_]", "").toLowerCase();
    }
}