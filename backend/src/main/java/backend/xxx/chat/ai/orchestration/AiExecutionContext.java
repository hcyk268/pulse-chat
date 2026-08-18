package backend.xxx.chat.ai.orchestration;

public record AiExecutionContext(
        String requestId,
        AiUseCaseType useCase,
        AiExecutionBudget budget,
        Long currentUserId,
        String currentUsername,
        String displayName,
        String locale
) {
}