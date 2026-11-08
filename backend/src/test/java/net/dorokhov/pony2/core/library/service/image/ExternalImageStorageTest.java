package net.dorokhov.pony2.core.library.service.image;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.ExternalImageFile;
import net.dorokhov.pony2.core.ProxiedHttpClientFactory;
import net.dorokhov.pony2.core.library.repository.ExternalImageRepository;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.search.backend.directory.root=${pony.home}/external-image-storage-index")
class ExternalImageStorageTest extends IntegrationTest {

    @Autowired private ExternalImageStorage externalImageStorage;
    @Autowired private ExternalImageRepository externalImageRepository;
    @Autowired private FileTypeResolver fileTypeResolver;
    @Value("${pony.externalImage.path}") private File externalImageFolder;

    private static final HttpServer proxyServer = createProxyServer();
    private byte[] content;
    private URI sourceUri;
    private final AtomicInteger downloads = new AtomicInteger();
    private final AtomicReference<URI> lastProxyRequest = new AtomicReference<>();
    private final AtomicReference<URI> redirectTarget = new AtomicReference<>();

    private static HttpServer createProxyServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void configureProxy(DynamicPropertyRegistry registry) {
        registry.add("pony.proxy.url", () -> "http://127.0.0.1:" + proxyServer.getAddress().getPort());
    }

    @BeforeEach
    void setUpServer() throws IOException {
        content = Files.readAllBytes(new ClassPathResource("image.png").getFile().toPath());
        proxyServer.createContext("/", exchange -> {
            lastProxyRequest.set(exchange.getRequestURI());
            switch (exchange.getRequestURI().getPath()) {
                case "/image" -> {
                    downloads.incrementAndGet();
                    exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                    respond(exchange, 200, content);
                }
                case "/invalid" -> respond(exchange, 200, "Not an image".getBytes(UTF_8));
                case "/redirect" -> {
                    exchange.getResponseHeaders().set("Location", redirectTarget.get().toString());
                    respond(exchange, 302, new byte[0]);
                }
                case "/blocked" -> respond(exchange, 403, new byte[0]);
                default -> respond(exchange, 404, new byte[0]);
            }
        });
        sourceUri = URI.create("http://images.invalid/image");
    }

    private void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        try (exchange) {
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            exchange.getResponseBody().write(body);
        }
    }

    @AfterEach
    void removeProxyHandler() {
        proxyServer.removeContext("/");
    }

    @AfterAll
    static void stopProxyServer() {
        proxyServer.stop(0);
    }

    @Test
    void shouldDownloadOriginalFileAndSaveMetadataWithoutDeduplication() throws IOException {
        ExternalImageFile first = externalImageStorage.downloadAndSave(sourceUri);
        ExternalImageFile second = externalImageStorage.downloadAndSave(sourceUri);

        assertThat(Files.readAllBytes(first.getFile().toPath())).isEqualTo(content);
        assertThat(Files.readAllBytes(second.getFile().toPath())).isEqualTo(content);
        assertThat(first.getExternalImage().getSourceUri()).isEqualTo(sourceUri);
        assertThat(first.getExternalImage().getMimeType()).isEqualTo("image/png");
        assertThat(first.getExternalImage().getSize()).isEqualTo(content.length);
        assertThat(first.getExternalImage().getDate()).isNotNull();
        assertThat(first.getExternalImage().getId()).isNotEqualTo(second.getExternalImage().getId());
        assertThat(first.getFile()).isNotEqualTo(second.getFile());
        assertThat(downloads.get()).isEqualTo(2);
        assertThat(lastProxyRequest.get()).isEqualTo(sourceUri);
        assertThat(externalImageRepository.count()).isEqualTo(2);
        assertThat(externalImageStorage.getImageFile(first.getExternalImage().getId())).get().satisfies(loaded -> {
            assertThat(loaded.getExternalImage().getId()).isEqualTo(first.getExternalImage().getId());
            assertThat(loaded.getFile()).isEqualTo(first.getFile());
        });
        assertThat(externalImageStorage.getImageFile("missing")).isEmpty();
    }

    @Test
    void shouldRemoveDownloadedFileWhenTransactionIsRolledBack() {
        ExternalImageFile externalImageFile = requireNonNull(getTransactionTemplate().execute(status -> {
            try {
                ExternalImageFile saved = externalImageStorage.downloadAndSave(sourceUri);
                assertThat(saved.getFile()).exists();
                status.setRollbackOnly();
                return saved;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));

        assertThat(externalImageRepository.count()).isZero();
        assertThat(externalImageFile.getFile()).doesNotExist();
    }

    @Test
    void shouldRemoveFileOnlyAfterDeletionIsCommitted() throws IOException {
        ExternalImageFile externalImageFile = externalImageStorage.downloadAndSave(sourceUri);
        String externalImageId = externalImageFile.getExternalImage().getId();

        getTransactionTemplate().executeWithoutResult(status -> {
            externalImageStorage.delete(externalImageId);
            assertThat(externalImageFile.getFile()).exists();
            status.setRollbackOnly();
        });
        assertThat(externalImageRepository.existsById(externalImageId)).isTrue();
        assertThat(externalImageFile.getFile()).exists();

        getTransactionTemplate().executeWithoutResult(status -> {
            externalImageStorage.delete(externalImageId);
            assertThat(externalImageFile.getFile()).exists();
        });
        assertThat(externalImageRepository.existsById(externalImageId)).isFalse();
        assertThat(externalImageFile.getFile()).doesNotExist();
        externalImageStorage.delete(externalImageId);
    }

    @Test
    void shouldNotSaveFailedDownloadOrNonImageResponse() throws IOException {
        assertThatThrownBy(() -> externalImageStorage.downloadAndSave(sourceUri.resolve("/missing")))
                .isInstanceOf(IOException.class).hasMessageContaining("404");
        assertThatThrownBy(() -> externalImageStorage.downloadAndSave(sourceUri.resolve("/invalid")))
                .isInstanceOf(IOException.class).hasMessageContaining("not an image");

        assertThat(externalImageRepository.count()).isZero();
        if (externalImageFolder.exists()) {
            try (Stream<Path> files = Files.walk(externalImageFolder.toPath())) {
                assertThat(files.filter(Files::isRegularFile)).isEmpty();
            }
        }
    }

    @Test
    void shouldSendRedirectThroughProxyAndRespectDenial() throws IOException {
        AtomicInteger directRequests = new AtomicInteger();
        HttpServer originServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        originServer.createContext("/blocked", exchange -> {
            directRequests.incrementAndGet();
            respond(exchange, 200, content);
        });
        originServer.start();
        try {
            URI target = URI.create("http://127.0.0.1:" + originServer.getAddress().getPort() + "/blocked");
            redirectTarget.set(target);

            assertThatThrownBy(() -> externalImageStorage.downloadAndSave(sourceUri.resolve("/redirect")))
                    .isInstanceOf(IOException.class).hasMessageContaining("403");

            assertThat(lastProxyRequest.get()).isEqualTo(target);
            assertThat(directRequests.get()).isZero();
            assertThat(externalImageRepository.count()).isZero();
        } finally {
            originServer.stop(0);
        }
    }

    @Test
    void shouldNotConnectDirectlyWhenProxyIsUnavailable() throws IOException {
        AtomicInteger directRequests = new AtomicInteger();
        HttpServer originServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        originServer.createContext("/image", exchange -> {
            directRequests.incrementAndGet();
            respond(exchange, 200, content);
        });
        originServer.start();
        try {
            int unavailableProxyPort;
            try (ServerSocket socket = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
                unavailableProxyPort = socket.getLocalPort();
            }
            ExternalImageStorage storage = new ExternalImageStorage(externalImageRepository, fileTypeResolver,
                    new ProxiedHttpClientFactory(URI.create("http://127.0.0.1:" + unavailableProxyPort)), externalImageFolder);
            URI target = URI.create("http://127.0.0.1:" + originServer.getAddress().getPort() + "/image");

            assertThatThrownBy(() -> storage.downloadAndSave(target)).isInstanceOf(IOException.class);

            assertThat(directRequests.get()).isZero();
            assertThat(externalImageRepository.count()).isZero();
        } finally {
            originServer.stop(0);
        }
    }
}
