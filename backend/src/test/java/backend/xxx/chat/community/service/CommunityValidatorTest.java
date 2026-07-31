package backend.xxx.chat.community.service;

import java.util.List;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.community.dto.CreateCommunityChannelRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityValidatorTest {

    private final CommunityValidator communityValidator = new CommunityValidator();

    @Test
    void keepsEmptySearchNull() {
        assertThat(communityValidator.normalizeSearchPattern(null)).isNull();
        assertThat(communityValidator.normalizeSearchPattern("   ")).isNull();
    }

    @Test
    void normalizesSearch() {
        assertThat(communityValidator.normalizeSearchPattern("  BitCoin  "))
                .isEqualTo("%bitcoin%");
    }

    @Test
    void normalizesLimit() {
        assertThat(communityValidator.normalizeLimit(null)).isEqualTo(20);
        assertThat(communityValidator.normalizeLimit((short) 50)).isEqualTo(50);
        assertThat(communityValidator.normalizeLimit((short) 51)).isEqualTo(50);
    }

    @Test
    void rejectsInvalidLimit() {
        assertThatThrownBy(() -> communityValidator.normalizeLimit((short) 0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void addsDefaultChannel() {
        List<CreateCommunityChannelRequest> channels =
                communityValidator.normalizeCreateChannelRequests(null);

        assertThat(channels).singleElement().satisfies(channel -> {
            assertThat(channel.name()).isEqualTo("General");
            assertThat(channel.readOnly()).isFalse();
        });
    }
}
