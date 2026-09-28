package net.dorokhov.pony2.core.llm;

import net.dorokhov.pony2.common.JsonConverter;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

@Component
public class FetchUrlTool {

    private static final int MAX_TEXT_LENGTH = 75000;

    private final ObjectProvider<ToolCallbackProvider> toolCallbackProviders;

    public FetchUrlTool(ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        this.toolCallbackProviders = toolCallbackProviders;
    }

    @Tool(
            name = "fetch_url",
            description = "Fetch a web page through the Playwright browser. Use this when a prompt asks to read a URL; it navigates with the same browser/proxy path as the Playwright MCP tools."
    )
    public String fetchUrl(
            @ToolParam(description = "Absolute http or https URL to open with Playwright.")
            String url
    ) {
        validateUrl(url);
        callPlaywrightTool("browser_navigate", JsonConverter.toJson(Map.of("url", url)));
        return callPlaywrightTool("browser_evaluate", JsonConverter.toJson(Map.of(
                "function", """
                        () => {
                          const body = document.body;
                          const text = body && body.innerText ? body.innerText : "";
                          return {
                            url: window.location.href,
                            title: document.title,
                            text: text.slice(0, %d)
                          };
                        }
                        """.formatted(MAX_TEXT_LENGTH)
        )));
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank.");
        }
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only http and https URLs are supported.");
        }
    }

    private String callPlaywrightTool(String toolName, String input) {
        return toolCallbackProviders.orderedStream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .filter(callback -> callback.getToolDefinition().name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Playwright MCP tool is not available: " + toolName))
                .call(input);
    }
}
