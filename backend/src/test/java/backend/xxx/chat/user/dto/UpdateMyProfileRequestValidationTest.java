package backend.xxx.chat.user.dto;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateMyProfileRequestValidationTest {

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
    void acceptsSafeProfileFields() {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest(
                "Alice Nguyen",
                "https://cdn.example.com/avatar.png",
                "Backend engineer"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsDisplayNameAndBioWithHtmlAngleBrackets() {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest(
                "Alice <b>",
                "https://cdn.example.com/avatar.png",
                "<script>alert(1)</script>"
        );

        assertThat(violatedFields(request)).contains("displayName", "bio");
    }

    @Test
    void rejectsNonHttpAvatarUrls() {
        UpdateMyProfileRequest javascriptUrl = new UpdateMyProfileRequest(
                "Alice",
                "javascript:alert(1)",
                "Backend engineer"
        );
        UpdateMyProfileRequest dataUrl = new UpdateMyProfileRequest(
                "Alice",
                "data:image/svg+xml,<svg onload=alert(1)>",
                "Backend engineer"
        );

        assertThat(violatedFields(javascriptUrl)).contains("avatarUrl");
        assertThat(violatedFields(dataUrl)).contains("avatarUrl");
    }

    @Test
    void allowsBlankAvatarUrlSoProfileNormalizationCanClearIt() {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest(
                "Alice",
                "",
                "Backend engineer"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    private Set<String> violatedFields(UpdateMyProfileRequest request) {
        return validator.validate(request)
                .stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
