package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.common.JsonConverter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;

@Component
public class FetchUrlTool {

    private final PlaywrightMcpCaller playwrightMcpCaller;
    private final int maxTextLength;

    public FetchUrlTool(
            PlaywrightMcpCaller playwrightMcpCaller,
            @Value("${pony.llm.fetchUrl.maxTextLength:75000}") int maxTextLength
    ) {
        if (maxTextLength < 1) {
            throw new IllegalArgumentException("Max text length must be positive.");
        }
        this.playwrightMcpCaller = playwrightMcpCaller;
        this.maxTextLength = maxTextLength;
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
        playwrightMcpCaller.call("browser_navigate", JsonConverter.toJson(Map.of("url", url)));
        return playwrightMcpCaller.call("browser_evaluate", JsonConverter.toJson(Map.of(
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
                        """.formatted(maxTextLength)
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
}
