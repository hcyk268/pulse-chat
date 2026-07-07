package backend.xxx.chat.common.validation;

public final class InputValidationPatterns {

    public static final String USERNAME = "^[A-Za-z0-9._-]+$";
    public static final String NO_HTML_ANGLE_BRACKETS = "^[^<>]*$";
    public static final String OPTIONAL_HTTP_URL = "^(?:$|https?://\\S+)$";

    private InputValidationPatterns() {
    }
}
