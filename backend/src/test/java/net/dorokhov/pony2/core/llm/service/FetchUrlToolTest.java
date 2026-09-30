package net.dorokhov.pony2.core.llm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FetchUrlToolTest {

    private FetchUrlTool fetchUrlTool;

    @Mock
    private PlaywrightMcpClient playwrightMcpClient;

    @Mock
    private RateLimitedRequestExecutor rateLimitedRequestExecutor;

    @BeforeEach
    void setUp() {
        fetchUrlTool = new FetchUrlTool(playwrightMcpClient, rateLimitedRequestExecutor, 123);
    }

    @Test
    public void shouldFetchUrlThroughPlaywrightMcpClient() {

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PlaywrightMcpClient.ToolCall>> toolCallsCaptor = ArgumentCaptor.forClass(List.class);
        doAnswer(invocation -> invocation.<Supplier<String>>getArgument(1).get())
                .when(rateLimitedRequestExecutor).execute(any(), any());
        when(playwrightMcpClient.call(ArgumentMatchers.any()))
                .thenReturn(List.of("navigateResult", "result"));

        String result = fetchUrlTool.fetchUrl("https://www.example.com");

        assertThat(result).isEqualTo("result");
        verify(rateLimitedRequestExecutor).execute(eq("example.com"), any());
        verify(playwrightMcpClient).call(toolCallsCaptor.capture());
        assertThat(toolCallsCaptor.getValue())
                .extracting(PlaywrightMcpClient.ToolCall::name)
                .containsExactly("browser_navigate", "browser_evaluate");
        assertThat(toolCallsCaptor.getValue().get(0).input()).isEqualTo("{\"url\":\"https://www.example.com\"}");
        assertThat(toolCallsCaptor.getValue().get(1).input()).contains("text.slice(0, 123)");
    }

    @Test
    public void shouldUseTopPrivateDomainAsRateLimitContext() {

        fetchUrlTool.fetchUrl("https://docs.google.co.uk/path");

        verify(rateLimitedRequestExecutor).execute(eq("google.co.uk"), any());
    }
}
