package backend.xxx.chat.ai.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import backend.xxx.chat.ai.dto.CommunityModerationDecision;
import backend.xxx.chat.ai.dto.CommunityModerationRequest;
import backend.xxx.chat.ai.dto.CommunityModerationResponse;
import backend.xxx.chat.ai.orchestration.AiExecutionContext;
import backend.xxx.chat.ai.orchestration.AiExecutionContextFactory;
import backend.xxx.chat.ai.orchestration.AiOrchestrator;
import backend.xxx.chat.ai.orchestration.AiUseCaseType;
import backend.xxx.chat.ai.prompt.InstructionPrompt;
import backend.xxx.chat.ai.safety.AiResponseValidator;
import backend.xxx.chat.ai.safety.SensitiveDataRedactor;
import backend.xxx.chat.community.dto.CommunityCategoryResponse;
import backend.xxx.chat.community.dto.CommunityDetailResponse;
import backend.xxx.chat.community.dto.CommunityTagResponse;
import backend.xxx.chat.community.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CommunityModerationUseCase {

    private final AiExecutionContextFactory contextFactory;
    private final AiOrchestrator orchestrator;
    private final AiResponseValidator responseValidator;
    private final SensitiveDataRedactor redactor;
    private final CommunityService communityService;

    public CommunityModerationResponse moderate(CommunityModerationRequest request) {
        AiExecutionContext context = contextFactory.create(AiUseCaseType.COMMUNITY_MODERATION);
        List<CommunityCategoryResponse> categories = communityService.getCategories();
        List<CommunityTagResponse> tags = communityService.getTags();
        CommunityDetailResponse community = StringUtils.hasText(request.communitySlug())
                ? communityService.getCommunityDetail(context.currentUsername(), request.communitySlug())
                : null;
        return generateModeration(context, request, community, categories, tags);
    }

    private CommunityModerationResponse generateModeration(
            AiExecutionContext context,
            CommunityModerationRequest request,
            CommunityDetailResponse community,
            List<CommunityCategoryResponse> categories,
            List<CommunityTagResponse> tags
    ) {
        ModerationInput input = new ModerationInput(request.title(), request.content(), community, categories, tags);

        var structured = orchestrator.completeStructuredTask(
                context,
                InstructionPrompt.COMMUNITY_MODERATION_PROMPT,
                input,
                CommunityModerationOutput.class
        );
        CommunityModerationOutput output = structured.entity();
        CommunityModerationDecision decision = output.decision() == null
                ? CommunityModerationDecision.REVIEW
                : output.decision();
        return new CommunityModerationResponse(
                decision,
                redactor.redact(responseValidator.requiredPlainText(output.reason(), 1_000)),
                validCategorySlug(boundedText(output.categorySlug(), 120), categories),
                validTagSlugs(boundedList(output.suggestedTags(), 8, 120), tags),
                Instant.now(),
                structured.model()
        );
    }

    private String boundedText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength).trim();
    }

    private List<String> boundedList(List<String> values, int maxItems, int maxItemLength) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .limit(maxItems)
                .map(value -> value.length() <= maxItemLength ? value.trim() : value.substring(0, maxItemLength).trim())
                .toList();
    }
    private String validCategorySlug(String categorySlug, List<CommunityCategoryResponse> categories) {
        if (!StringUtils.hasText(categorySlug)) {
            return null;
        }
        Set<String> knownSlugs = categories.stream()
                .map(CommunityCategoryResponse::slug)
                .map(slug -> slug.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        String normalizedSlug = categorySlug.toLowerCase(Locale.ROOT);
        return knownSlugs.contains(normalizedSlug) ? normalizedSlug : null;
    }

    private List<String> validTagSlugs(List<String> suggestedTags, List<CommunityTagResponse> tags) {
        Set<String> knownSlugs = tags.stream()
                .map(CommunityTagResponse::slug)
                .map(slug -> slug.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return suggestedTags.stream()
                .map(slug -> slug.toLowerCase(Locale.ROOT))
                .filter(knownSlugs::contains)
                .distinct()
                .toList();
    }

    private record ModerationInput(
            String title,
            String content,
            CommunityDetailResponse community,
            List<CommunityCategoryResponse> categories,
            List<CommunityTagResponse> tags
    ) {
    }
}