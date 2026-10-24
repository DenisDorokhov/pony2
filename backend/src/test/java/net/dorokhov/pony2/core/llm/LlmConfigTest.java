package net.dorokhov.pony2.core.llm;

import io.micrometer.observation.ObservationRegistry;
import net.dorokhov.pony2.core.llm.service.ChatModelImpl;
import net.dorokhov.pony2.core.llm.service.FetchUrlTool;
import net.dorokhov.pony2.core.llm.service.McpToolArgumentValidator;
import net.dorokhov.pony2.core.llm.service.PlaywrightMcpToolRateLimiter;
import net.dorokhov.pony2.core.llm.service.RateLimitedPlaywrightMcpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
                new PlaywrightMcpToolRateLimiter(rateLimitedPlaywrightMcpClient),
                new McpToolArgumentValidator(3), toolCallbackProviders);
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

    @Test
    void shouldLetModelCorrectArgumentsAndPreserveConversationHistory() {
        ToolCallback browser = tool("browser_evaluate");
        String arguments = "{\"function\":\"() => document.title\"}";
        when(rateLimitedPlaywrightMcpClient.call(eq(browser), eq(arguments), any())).thenReturn("Spotify page title");
        BiFunction<OpenAiHttpClientBuilderCustomizer, Boolean, ChatClient> factory = factory(browser);
        when(model.call(any(Prompt.class))).thenReturn(
                toolResponse("broken-call", "browser_evaluate", "{\"function\":\"() => document.title"),
                toolResponse("corrected-call", "browser_evaluate", arguments),
                new ChatResponse(List.of(new Generation(new AssistantMessage("Collected artist data")))));

        assertThat(factory.apply(builder -> {}, true).prompt("Collect artist data").call().content())
                .isEqualTo("Collected artist data");

        ArgumentCaptor<Prompt> prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(3)).call(prompts.capture());
        List<Message> correctionHistory = prompts.getAllValues().get(1).getInstructions();
        assertThat(correctionHistory.getFirst().getText()).isEqualTo("Collect artist data");
        assertThat(correctionHistory).filteredOn(AssistantMessage.class::isInstance)
                .singleElement().satisfies(message -> assertThat(((AssistantMessage) message).getToolCalls())
                        .singleElement().satisfies(call -> assertThat(call.id()).isEqualTo("broken-call")));
        assertThat(correctionHistory).filteredOn(ToolResponseMessage.class::isInstance)
                .singleElement().satisfies(message -> assertThat(((ToolResponseMessage) message).getResponses())
                        .singleElement().satisfies(response -> {
                            assertThat(response.id()).isEqualTo("broken-call");
                            assertThat(response.name()).isEqualTo("browser_evaluate");
                            assertThat(response.responseData()).contains("The tool was not executed", "Correction 1 of 3");
                        }));
        assertThat(prompts.getAllValues().getLast().getInstructions()).filteredOn(ToolResponseMessage.class::isInstance)
                .hasSize(2);
        verify(rateLimitedPlaywrightMcpClient, times(1)).call(eq(browser), any(), any());
    }

    private BiFunction<OpenAiHttpClientBuilderCustomizer, Boolean, ChatClient> factory(ToolCallback... callbacks) {
        when(toolCallbackProviders.orderedStream()).thenAnswer(invocation -> Stream.<ToolCallbackProvider>of(() -> callbacks));
        when(model.createChatModel(any())).thenReturn(model);
        when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        return new LlmConfig().llmChatClientFactory(model, ObservationRegistry.NOOP, fetchUrlTool,
                new PlaywrightMcpToolRateLimiter(rateLimitedPlaywrightMcpClient),
                new McpToolArgumentValidator(3), toolCallbackProviders);
    }

    private ChatResponse toolResponse(String id, String name, String arguments) {
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build())));
    }

    @Test
    void shouldValidateOtherMcpToolsAndIsolateConversationBudgets() {
        ToolCallback selenium = mock(ToolCallback.class);
        when(selenium.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name("selenium_execute_script").description("Execute JavaScript").inputSchema("{}").build());
        when(selenium.getToolMetadata()).thenReturn(ToolMetadata.builder().build());
        BiFunction<OpenAiHttpClientBuilderCustomizer, Boolean, ChatClient> factory = factory(selenium);
        when(model.call(any(Prompt.class))).thenReturn(toolResponse("broken-call", "selenium_execute_script", "{"));
        ChatClient first = factory.apply(builder -> {}, true);
        ChatClient second = factory.apply(builder -> {}, true);

        assertThatThrownBy(() -> first.prompt("First artist").call().content())
                .isExactlyInstanceOf(IllegalStateException.class).hasMessageContaining("correction limit (3) exceeded");
        assertThatThrownBy(() -> second.prompt("Second artist").call().content())
                .isExactlyInstanceOf(IllegalStateException.class).hasMessageContaining("correction limit (3) exceeded");

        verify(model, times(8)).call(any(Prompt.class));
        verify(selenium, never()).call(any(), any());
        verify(rateLimitedPlaywrightMcpClient, never()).call(any(ToolCallback.class), any(), any());
    }
}
