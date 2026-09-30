package net.dorokhov.pony2.core.llm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FetchUrlToolTest {

    private FetchUrlTool fetchUrlTool;

    @Mock
    private PlaywrightMcpCaller playwrightMcpCaller;

    @BeforeEach
    void setUp() {
        fetchUrlTool = new FetchUrlTool(playwrightMcpCaller, 123);
    }

    @Test
    public void shouldFetchUrlThroughPlaywrightToolCaller() {

        ArgumentCaptor<String> evaluateInputCaptor = ArgumentCaptor.forClass(String.class);
        when(playwrightMcpCaller.call(anyString(), anyString())).thenAnswer(invocation ->
                "browser_evaluate".equals(invocation.getArgument(0)) ? "result" : null);

        String result = fetchUrlTool.fetchUrl("https://example.com");

        assertThat(result).isEqualTo("result");
        verify(playwrightMcpCaller).call("browser_navigate", "{\"url\":\"https://example.com\"}");
        verify(playwrightMcpCaller).call(eq("browser_evaluate"), evaluateInputCaptor.capture());
        assertThat(evaluateInputCaptor.getValue()).contains("text.slice(0, 123)");
    }
}
