package net.dorokhov.pony2.core.llm.service;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.llm.service.PlaywrightMcpClient.ToolCall;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class FetchUrlTool {

    private final PlaywrightMcpClient playwrightMcpClient;
    private final RateLimitedRequestExecutor rateLimitedRequestExecutor;
    private final int maxTextLength;

    @Autowired
    public FetchUrlTool(
            PlaywrightMcpClient playwrightMcpClient,
            @Value("${pony.llm.fetchUrl.rateLimit.notMoreOftenThan:2s}") Duration requestInterval,
            @Value("${pony.llm.fetchUrl.rateLimit.randomDelay:4s}") Duration requestRandomDelay,
            @Value("${pony.llm.fetchUrl.rateLimit.retriesOnException:1}") int retriesOnException,
            @Value("${pony.llm.fetchUrl.rateLimit.maxContextCount:1000}") int maxContextCount,
            @Value("${pony.llm.fetchUrl.maxTextLength:75000}") int maxTextLength
    ) {
        this(
                playwrightMcpClient,
                new RateLimitedRequestExecutor(
                        "fetch-url",
                        new RateLimitedRequestExecutor.Settings(
                                requestInterval,
                                requestRandomDelay,
                                retriesOnException,
                                maxContextCount
                        )
                ),
                maxTextLength
        );
    }

    FetchUrlTool(
            PlaywrightMcpClient playwrightMcpClient,
            RateLimitedRequestExecutor rateLimitedRequestExecutor,
            int maxTextLength
    ) {
        if (maxTextLength < 1) {
            throw new IllegalArgumentException("Max text length must be positive.");
        }
        this.playwrightMcpClient = playwrightMcpClient;
        this.maxTextLength = maxTextLength;
        this.rateLimitedRequestExecutor = rateLimitedRequestExecutor;
    }

    @Tool(
            name = "fetch_url",
            description = "Fetch a web page through the Playwright browser. Use this when a prompt asks to read a URL; it navigates with the same browser/proxy path as the Playwright MCP tools."
    )
    public String fetchUrl(
            @ToolParam(description = "Absolute http or https URL to open with Playwright.")
            String url
    ) {
        URI uri = validateUrl(url);
        String navigateInput = JsonConverter.toJson(Map.of("url", url));
        String evaluateInput = JsonConverter.toJson(Map.of(
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
        ));
        return rateLimitedRequestExecutor.execute(rateLimitContext(uri), () ->
                playwrightMcpClient.call(List.of(
                        new ToolCall("browser_navigate", navigateInput),
                        new ToolCall("browser_evaluate", evaluateInput)
                )).get(1)
        );
    }

    private URI validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank.");
        }
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only http and https URLs are supported.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URL host must not be blank.");
        }
        return uri;
    }

    private String rateLimitContext(URI uri) {
        String host = removeTrailingDot(uri.getHost()).toLowerCase(Locale.ROOT);
        if (InetAddresses.isInetAddress(host)) {
            return host;
        }
        try {
            String asciiHost = IDN.toASCII(host).toLowerCase(Locale.ROOT);
            InternetDomainName domainName = InternetDomainName.from(asciiHost);
            if (domainName.isUnderPublicSuffix()) {
                return domainName.topPrivateDomain().toString();
            }
            return asciiHost;
        } catch (IllegalArgumentException ignored) {
            // Fall back to the normalized host for localhost and private/internal names.
        }
        return host;
    }

    private String removeTrailingDot(String host) {
        return host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
    }
}
