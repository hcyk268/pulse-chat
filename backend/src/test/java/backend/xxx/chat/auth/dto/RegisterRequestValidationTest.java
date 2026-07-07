package backend.xxx.chat.auth.dto;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsSafeUsernameAndDisplayName() {
        RegisterRequest request = new RegisterRequest(
                "alice._-123",
                "alice@example.com",
                "Alice Nguyen",
                "password123",
                "password123"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsUsernameWithUnsupportedCharacters() {
        RegisterRequest request = new RegisterRequest(
                "alice<script>",
                "alice@example.com",
                "Alice Nguyen",
                "password123",
                "password123"
        );

        assertThat(violatedFields(request)).contains("username");
    }

    @Test
    void rejectsDisplayNameWithHtmlAngleBrackets() {
        RegisterRequest request = new RegisterRequest(
                "alice",
                "alice@example.com",
                "<img src=x onerror=alert(1)>",
                "password123",
                "password123"
        );

        assertThat(violatedFields(request)).contains("displayName");
    }

    private Set<String> violatedFields(RegisterRequest request) {
        return validator.validate(request)
                .stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
