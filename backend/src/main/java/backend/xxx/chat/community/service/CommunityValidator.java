package backend.xxx.chat.community.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.community.dto.CreateCommunityChannelRequest;
import backend.xxx.chat.community.model.Community;
import backend.xxx.chat.community.model.CommunityChannelType;
import org.springframework.stereotype.Component;

@Component
public class CommunityValidator {

    private static final int DEFAULT_COMMUNITY_LIMIT = 20;
    private static final int MAX_COMMUNITY_LIMIT = 50;
    private static final int MAX_CHANNELS_PER_CREATE = 20;
    private static final int NAME_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 1000;
    private static final int CHANNEL_DESCRIPTION_MAX_LENGTH = 500;
    private static final Pattern NON_ASCII_MARKS = Pattern.compile("\\p{M}+");

    public int normalizeLimit(Short limit) {
        if (limit == null) {
            return DEFAULT_COMMUNITY_LIMIT;
        }
        if (limit < 1) {
            throw new ValidationException("community.limit.min");
        }
        return Math.min(limit, MAX_COMMUNITY_LIMIT);
    }

    public String normalizeRequiredCommunityName(String value) {
        return normalizeRequiredText(value, "community.name", NAME_MAX_LENGTH);
    }

    public String normalizeOptionalCommunityDescription(String value) {
        return normalizeOptionalText(value, DESCRIPTION_MAX_LENGTH);
    }

    public String normalizeRequiredChannelName(String value) {
        return normalizeRequiredText(value, "community.channel.name", NAME_MAX_LENGTH);
    }

    public String normalizeOptionalChannelDescription(String value) {
        return normalizeOptionalText(value, CHANNEL_DESCRIPTION_MAX_LENGTH);
    }

    public String normalizeSearchPattern(String value) {
        String normalized = normalizeOptionalText(value, DESCRIPTION_MAX_LENGTH);
        return normalized == null ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }

    public String normalizeRequiredSlug(String value, String fieldName) {
        String slug = normalizeOptionalSlug(value);
        if (slug == null) {
            throw new ValidationException(fieldName + ".blank");
        }
        return slug;
    }

    public String normalizeOptionalSlug(String value) {
        if (value == null) {
            return null;
        }
        String ascii = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        ascii = NON_ASCII_MARKS.matcher(ascii).replaceAll("");
        String slug = ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? null : slug;
    }

    public Set<String> normalizeSlugSet(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .map(this::normalizeOptionalSlug)
                .filter(slug -> slug != null && !slug.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<CreateCommunityChannelRequest> normalizeCreateChannelRequests(
            List<CreateCommunityChannelRequest> requests
    ) {
        List<CreateCommunityChannelRequest> normalized =
                requests == null ? new ArrayList<>() : new ArrayList<>(requests);
        if (normalized.isEmpty()) {
            normalized.add(new CreateCommunityChannelRequest(
                    "General",
                    "Market context, chart reads, and desk notes.",
                    CommunityChannelType.TEXT,
                    false
            ));
        }
        if (normalized.size() > MAX_CHANNELS_PER_CREATE) {
            throw new ValidationException("community.channels.limit.exceeded");
        }
        return normalized;
    }

    public void validateCommunityId(Long communityId) {
        if (communityId == null) {
            throw new ValidationException("community.id.required");
        }
    }

    public void validateActiveCommunity(Community community) {
        if (!community.isActive()) {
            throw new NotFoundException("community.not.found");
        }
    }

    private String normalizeRequiredText(String value, String fieldName, int maxLength) {
        String normalized = normalizeOptionalText(value, maxLength);
        if (normalized == null) {
            throw new ValidationException(fieldName + ".blank");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new ValidationException("validation.field.max.length");
        }
        return normalized;
    }
}
