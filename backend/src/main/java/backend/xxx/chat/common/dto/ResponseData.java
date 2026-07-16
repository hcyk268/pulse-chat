package backend.xxx.chat.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import backend.xxx.chat.common.web.Translator;
import lombok.Getter;

@Getter
public class ResponseData<T> {
    private final boolean success;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public ResponseData(boolean success, String message, T data) {
        this.success = success;
        this.message = Translator.toLocale(message);
        this.data = data;
    }

    public ResponseData(boolean success, String message) {
        this.success = success;
        this.message = Translator.toLocale(message);
    }
}
