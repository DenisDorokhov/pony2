package net.dorokhov.pony2.web;

import net.dorokhov.pony2.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpCompressionTest extends IntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    public void shouldGzipCompressTextResponses() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/index.html"))
                .header("Accept-Encoding", "gzip")
                .GET()
                .build();

        HttpResponse<byte[]> response;
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Encoding")).hasValue("gzip");
        assertThat(response.body()).startsWith((byte) 0x1f, (byte) 0x8b);

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
            String body = new String(gzipInputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(body).contains("<pony-root>");
        }
    }
}
