package net.dorokhov.pony2.core.llm.service;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.common.JsonConverter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatModelImplTest {

    @ParameterizedTest
    @MethodSource("modelParameters")
    void shouldSendOnlyConfiguredParameters(@Nullable Double temperature, @Nullable Double topP, @Nullable Integer topK,
                                          @Nullable Double minP, @Nullable Double presencePenalty, @Nullable Double repeatPenalty,
                                          Map<String, Object> expectedParameters) throws IOException {
        List<Map<String, Object>> requests = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try (exchange) {
                requests.add(JsonConverter.mapFromJson(new String(exchange.getRequestBody().readAllBytes(), UTF_8),
                        String.class, Object.class));
                byte[] response = """
                        {"id":"test","object":"chat.completion","created":1,"model":"test",
                         "choices":[{"index":0,"message":{"role":"assistant","content":"OK"},"finish_reason":"stop"}]}
                        """.getBytes(UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        server.start();
        try {
            ConfigService configService = mock(ConfigService.class);
            when(configService.get()).thenReturn(new ConfigSet(null, List.of(), true,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test", null));
            ChatModelImpl model = new ChatModelImpl(configService, ObservationRegistry.NOOP, Duration.ofSeconds(5),
                    temperature, topP, topK, minP, presencePenalty, repeatPenalty);

            assertThat(model.call(new Prompt("test")).getResult().getOutput().getText()).isEqualTo("OK");
            assertThat(ChatClient.create(model.createChatModel(builder -> {})).prompt("test")
                    .options(OpenAiChatOptions.builder().toolChoice("none")).call().content()).isEqualTo("OK");

            assertThat(requests).hasSize(2).allSatisfy(request -> {
                assertThat(request).containsAllEntriesOf(expectedParameters)
                        .doesNotContainKeys("topP", "topK", "minP", "presencePenalty", "repeatPenalty", "extra_body");
                for (String parameter : List.of("temperature", "top_p", "top_k", "min_p", "presence_penalty", "repeat_penalty")) {
                    if (!expectedParameters.containsKey(parameter)) {
                        assertThat(request).doesNotContainKey(parameter);
                    }
                }
            });
        } finally {
            server.stop(0);
        }
    }

    private static Stream<Arguments> modelParameters() {
        return Stream.of(
                Arguments.of(null, null, null, null, null, null, Map.of()),
                Arguments.of(1.0, 0.95, 20, 0.05, 0.0, 1.0, Map.of(
                        "temperature", 1.0, "top_p", 0.95, "top_k", 20, "min_p", 0.05,
                        "presence_penalty", 0.0, "repeat_penalty", 1.0)),
                Arguments.of(0.0, 0.0, 0, 0.0, 0.0, 0.0, Map.of(
                        "temperature", 0.0, "top_p", 0.0, "top_k", 0, "min_p", 0.0,
                        "presence_penalty", 0.0, "repeat_penalty", 0.0)),
                Arguments.of(null, null, null, 0.0, null, null, Map.of("min_p", 0.0)),
                Arguments.of(null, null, null, 0.05, null, null, Map.of("min_p", 0.05))
        );
    }
}
