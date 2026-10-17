package net.dorokhov.pony2.core.llm;

import io.micrometer.observation.ObservationRegistry;
import net.dorokhov.pony2.core.llm.service.ChatModelImpl;
import net.dorokhov.pony2.core.llm.service.FetchUrlTool;
import net.dorokhov.pony2.core.llm.service.PlaywrightMcpToolRateLimiter;
import net.dorokhov.pony2.core.llm.service.RateLimitedPlaywrightMcpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmConfigTest {

    @Mock private ChatModelImpl model;
    @Mock private FetchUrlTool fetchUrlTool;
    @Mock private RateLimitedPlaywrightMcpClient rateLimitedPlaywrightMcpClient;
    @Mock private ObjectProvider<ToolCallbackProvider> toolCallbackProviders;

    @Test
    void shouldExcludeScreenshotToolFromModelRequests() {
        ToolCallback screenshot = tool("browser_take_screenshot");
        ToolCallback snapshot = tool("browser_snapshot");
        ToolCallback evaluate = tool("browser_evaluate");
        when(toolCallbackProviders.orderedStream()).thenReturn(Stream.of(
                () -> new ToolCallback[]{screenshot, snapshot},
                () -> new ToolCallback[]{evaluate}));
        when(model.createChatModel(any())).thenReturn(model);
        when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage("OK")))));

        BiFunction<OpenAiHttpClientBuilderCustomizer, Boolean, ChatClient> factory = new LlmConfig().llmChatClientFactory(
                model, ObservationRegistry.NOOP, fetchUrlTool,
                new PlaywrightMcpToolRateLimiter(rateLimitedPlaywrightMcpClient), toolCallbackProviders);
        ChatClient client = factory.apply(builder -> {}, true);

        assertThat(client.prompt("Collect artist data").call().content()).isEqualTo("OK");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(prompt.capture());
        assertThat(((ToolCallingChatOptions) prompt.getValue().getOptions()).getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder("fetch_url", "browser_snapshot", "browser_evaluate");
    }

    private ToolCallback tool(String name) {
        return FunctionToolCallback.builder(name, (String input) -> input)
                .description(name + " description")
                .inputType(String.class)
                .build();
    }
}
