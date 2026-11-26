package net.dorokhov.pony2.core.llm.service;

import jakarta.annotation.Nullable;
import okhttp3.Interceptor;
import okhttp3.Request;

import java.io.IOException;

/**
 * Finds how many tokens a model request needs and lets the caller change its output token limit.
 * Each implementation handles one model-server API and uses the HTTP transport of the original request.
 */
public interface LlmContextService {

    /**
     * Counts the input tokens and reads the requested output token limit.
     * If knownContextSize is zero, asks the server for the context size.
     * Any server requests use the supplied HTTP chain, keeping the original settings and cancellation handling.
     * Returns null when the request does not need token counting or output limiting.
     */
    @Nullable PreparedRequest inspect(Interceptor.Chain chain, int knownContextSize) throws IOException;

    record ContextInfo(int contextSize, int inputTokens, int requestedOutputLimit, boolean hasToolHistory) {}

    interface PreparedRequest {

        ContextInfo context();

        Request withOutputLimit(int maxTokens);
    }
}
