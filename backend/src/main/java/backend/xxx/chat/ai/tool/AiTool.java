package backend.xxx.chat.ai.tool;

import backend.xxx.chat.ai.orchestration.AiExecutionContext;

public interface AiTool<I, O> {

    String name();

    String description();

    String argumentSchema();

    Class<I> inputType();

    AiToolAccess access();

    O execute(I input, AiExecutionContext context);
}