package backend.xxx.chat.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import backend.xxx.chat.common.exception.InternalServerErrorException;
import backend.xxx.chat.common.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CursorCodec {

    private final ObjectMapper objectMapper;

    public <T> T decode(String cursor, Class<T> cursorType, String invalidMessage) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String json = new String(
                    Base64.getDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );
            return objectMapper.readValue(json, cursorType);
        } catch (Exception exception) {
            throw new ValidationException(invalidMessage);
        }
    }

    public String encode(Object cursor, String failureMessage) {
        if (cursor == null) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(cursor);
            return Base64.getEncoder()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException exception) {
            throw new InternalServerErrorException(failureMessage);
        }
    }
}
