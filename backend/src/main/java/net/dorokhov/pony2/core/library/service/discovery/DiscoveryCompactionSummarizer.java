package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.common.TextTemplate;
import net.dorokhov.pony2.core.llm.service.exception.LlmContextLimitException;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.session.compaction.ChatModelSummarizer;
import org.springframework.ai.session.compaction.CompactionSummarizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Asks the model to summarize completed tool calls and their results.
 * Disables tools for the summary request and rejects failed or empty summaries.
 * Adds a user message after the summary so the next model request continues the original task.
 */
@Component
public class DiscoveryCompactionSummarizer {

    private final TextTemplate systemPrompt;
    private final TextTemplate resumeUserPrompt;

    public DiscoveryCompactionSummarizer(
            @Value("classpath:prompts/common-compaction-system.vm")
            Resource systemPrompt,
            @Value("classpath:prompts/common-compaction-resume-user.vm")
            Resource resumeUserPrompt
    ) throws IOException {
        this.systemPrompt = new TextTemplate(systemPrompt);
        this.resumeUserPrompt = new TextTemplate(resumeUserPrompt);
    }

    public CompactionSummarizer create(ChatModel model, @Nullable ChatOptions baseOptions, int overlapSize, Runnable checkCancellation) {
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder();
        if (baseOptions != null) {
            options.combineWith(baseOptions.mutate());
        }
        options.toolCallbacks(List.of()).toolChoice("none").reasoningEffort("none")
                .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.TEXT).build())
                .maxTokens(8192);
        ChatModelSummarizer summarizer = ChatModelSummarizer.builder(model).chatOptions(options.build())
                .systemPrompt(systemPrompt.render())
                .overlapSize(overlapSize)
                .onSummarizationFailure(summary -> {
                    checkCancellation.run();
                    throw new LlmContextLimitException(
                            "LLM compaction failed or returned an empty summary.");
                }).build();
        return request -> {
            List<Message> summary = summarizer.summarize(request);
            List<Message> messages = new ArrayList<>(summary);
            // llama.cpp can treat a final assistant message as text to continue. Add a user message to resume the task.
            messages.add(new UserMessage(resumeUserPrompt.render()));
            return messages;
        };
    }
}
