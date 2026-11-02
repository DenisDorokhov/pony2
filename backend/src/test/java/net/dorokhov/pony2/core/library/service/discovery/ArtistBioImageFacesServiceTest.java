package net.dorokhov.pony2.core.library.service.discovery;

import com.sun.net.httpserver.HttpServer;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.DiscoveryCacheService;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.library.service.exception.LlmFollowUpException;
import net.dorokhov.pony2.core.library.service.image.ExternalImageStorage;
import net.dorokhov.pony2.core.library.service.image.ImageSizeReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion.SPOTIFY;
import static net.dorokhov.pony2.api.library.domain.DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistBioImageFacesServiceTest {

    @Mock private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Mock private DiscoveryTaskRepository taskRepository;
    @Mock private ExternalImageStorage imageStorage;
    @Mock private ConfigService configService;
    @Mock private DiscoveryCacheService cacheService;
    @Mock private LogService logService;
    @TempDir private Path temporaryDirectory;

    private final LibraryJobSynchronizer jobSynchronizer = new LibraryJobSynchronizer(new ShutdownService());
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final Map<String, String> cache = new HashMap<>();
    private final List<String> responses = new CopyOnWriteArrayList<>();
    private LibraryJobSynchronizer.LibraryJobRegistration registration;
    private HttpServer server;
    private ConfigSet config;
    private ExternalImage image;
    private Path file;
    private Artist artist;
    private ArtistBioImageFacesService service;

    @BeforeEach
    void setUp() throws Exception {
        registration = jobSynchronizer.registerDiscoveryJob();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), UTF_8));
            String response = responses.isEmpty() ? "{\"faces\": [[100, 200, 300, 500], [600, 100, 900, 600]]}" : responses.removeFirst();
            byte[] body = JsonConverter.toJson(Map.of("id", "faces-test", "object", "chat.completion", "created", 0,
                    "model", "face-test", "choices", List.of(Map.of("index", 0, "finish_reason", "stop",
                            "message", Map.of("role", "assistant", "content", response))))).getBytes(UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        server.start();
        config = new ConfigSet(null, List.of(), true, "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "face-test", null);
        lenient().when(configService.get()).thenAnswer(invocation -> config);
        lenient().when(cacheService.get(eq(SPOTIFY), anyString(), eq(1)))
                .thenAnswer(invocation -> Optional.ofNullable(cache.get(invocation.getArgument(1))));
        lenient().when(cacheService.put(eq(SPOTIFY), anyString(), eq(1), anyString())).thenAnswer(invocation -> {
            cache.put(invocation.getArgument(1), invocation.getArgument(3));
            return null;
        });
        artist = new Artist().setId("artist");
        image = new ExternalImage();
        ReflectionTestUtils.setField(image, "id", "image");
        file = temporaryDirectory.resolve("image.png");
        try (InputStream input = new ClassPathResource("image.png").getInputStream()) {
            Files.copy(input, file);
        }
        lenient().when(artistDiscoveryRepository.findByArtistId("artist"))
                .thenReturn(Optional.of(new ArtistDiscovery().setArtist(artist).setBioExternalImage(image)));
        lenient().when(imageStorage.getImageFile("image")).thenAnswer(invocation -> Optional.of(new ExternalImageFile(image, file.toFile())));
        service = service(0);
    }

    private ArtistBioImageFacesService service(int retries) throws Exception {
        DiscoveryChatClient client = new DiscoveryChatClient((customizer, toolsEnabled) -> ChatClient.builder(
                OpenAiChatModel.builder().options(OpenAiChatOptions.builder().baseUrl(config.llmUrl())
                                .model(config.llmModel()).apiKey(new NoopApiKey()).build())
                        .httpClientBuilderCustomizer(customizer).build()).build(), jobSynchronizer, new DiscoveryAdvisor(jobSynchronizer));
        DiscoveryTaskLlmExecutor executor = new DiscoveryTaskLlmExecutor(client, cacheService,
                new DiscoveryTaskExchangeRecorder(taskRepository, transactionManager()), logService, jobSynchronizer,
                retries, 0, Duration.ofSeconds(30));
        return new ArtistBioImageFacesService(executor, artistDiscoveryRepository, taskRepository, imageStorage,
                new ImageSizeReader(), configService, transactionManager(), new ClassPathResource("prompts/artist-bio-image-faces.txt"));
    }

    @AfterEach
    void tearDown() {
        registration.close();
        server.stop(0);
    }

    @Test
    void shouldSendImageAndStrictSchemaAndNormalizeFaces() {
        DiscoveryTask task = task();

        ArtistBioImageFacesService.Result result = service.discover(task, true);

        assertThat(result).isNotNull();
        assertThat(result.externalImageId()).isEqualTo("image");
        assertThat(result.faces()).containsExactly(new ImageFace(0.1, 0.2, 0.3, 0.5), new ImageFace(0.6, 0.1, 0.9, 0.6));
        Map<String, Object> request = JsonConverter.mapFromJson(requests.getFirst(), String.class, Object.class);
        assertThat(request).containsEntry("model", "face-test").containsEntry("temperature", 0.0).containsEntry("tool_choice", "none");
        assertThat(request.get("tools")).isNull();
        Map<?, ?> format = (Map<?, ?>) request.get("response_format");
        assertThat(format.get("type")).isEqualTo("json_schema");
        Map<?, ?> schema = (Map<?, ?>) format.get("json_schema");
        assertThat(schema.get("strict")).isEqualTo(true);
        assertThat(JsonConverter.toJson(schema.get("schema"))).contains("\"faces\"", "\"array\"", "\"number\"")
                .doesNotContain("minimum", "maximum");
        assertThat(request.get("chat_template_kwargs")).isEqualTo(Map.of("enable_thinking", false));
        assertThat(requests.getFirst()).contains("data:image/png;base64,");
        assertThat(JsonConverter.toJson(task.getRawRequests())).contains("imageHash", "responseSchema").doesNotContain("data:image", "iVBOR");
        assertThat(cache).hasSize(1);
    }

    private DiscoveryTask task() {
        return new DiscoveryTask().setId("task").setArtist(artist).setType(ARTIST_BIO_IMAGE_FACES);
    }

    @Test
    void shouldAcceptAndCacheNoFaces() {
        responses.add("{\"faces\": []}");

        ArtistBioImageFacesService.Result first = service.discover(task(), true);
        ArtistBioImageFacesService.Result second = service.discover(task(), true);

        assertThat(first.faces()).isEmpty();
        assertThat(second).isEqualTo(first);
        assertThat(requests).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{\"faces\":null}", "{}", "{\"faces\":[null]}", "{\"faces\":[[1,2,3]]}",
            "{\"faces\":[[1,2,3,4,5]]}", "{\"faces\":[[-1,2,3,4]]}", "{\"faces\":[[1,2,1001,4]]}",
            "{\"faces\":[[1,2,1,4]]}", "{\"faces\":[[1,4,3,2]]}", "{\"faces\":[[1,2,null,4]]}",
            "{\"faces\":[[1,2,1e309,4]]}", "{\"faces\":[[1,2,\"invalid\",4]]}", "Not JSON"})
    void shouldRejectMalformedOrInvalidCoordinatesWithoutCaching(String response) {
        responses.add(response);

        assertThatThrownBy(() -> service.discover(task(), true)).isInstanceOf(LlmFollowUpException.class);

        assertThat(cache).isEmpty();
    }

    @Test
    void shouldKeepImageInFollowUpWhenCorrectingInvalidCoordinates() throws Exception {
        service = service(1);
        responses.add("{\"faces\": [[10,20,1001,200]]}");
        responses.add("{\"faces\": [[10,20,100,200]]}");

        assertThat(service.discover(task(), true).faces()).containsExactly(new ImageFace(0.01, 0.02, 0.1, 0.2));

        assertThat(requests).hasSize(2).allSatisfy(request -> assertThat(request).contains("data:image/png;base64,"));
        assertThat(cache).hasSize(1);
    }

    @Test
    void shouldNotCallModelWhenBiographyImageIsAbsent() {
        when(artistDiscoveryRepository.findByArtistId("artist")).thenReturn(Optional.empty());

        assertThat(service.discover(task(), true)).isNull();

        assertThat(requests).isEmpty();
    }

    @Test
    void shouldReuseOnlyAnalysisOfTheCurrentImageAndConfiguration() {
        ArtistBioImageFacesService.Result result = service.discover(task(), true);
        when(taskRepository.findLatestCompletedByArtistIdAndType("artist", ARTIST_BIO_IMAGE_FACES, Limit.of(1)))
                .thenReturn(Optional.of(task().setResult(JsonConverter.toJson(result))));

        assertThat(service.shouldDiscover(artist)).isFalse();

        config = new ConfigSet(null, List.of(), true, config.llmUrl(), "another-model", null);
        assertThat(service.shouldDiscover(artist)).isTrue();
        config = new ConfigSet(null, List.of(), true, "http://another-endpoint/v1", "face-test", null);
        assertThat(service.shouldDiscover(artist)).isTrue();
        config = new ConfigSet(null, List.of(), true, "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "face-test", null);
        when(taskRepository.findLatestCompletedByArtistIdAndType("artist", ARTIST_BIO_IMAGE_FACES, Limit.of(1)))
                .thenReturn(Optional.of(task().setResult(null)));
        assertThat(service.shouldDiscover(artist)).isTrue();
    }

    @Test
    void shouldInvalidateAnalysisWhenImageContentChanges() throws Exception {
        ArtistBioImageFacesService.Result result = service.discover(task(), true);
        when(taskRepository.findLatestCompletedByArtistIdAndType("artist", ARTIST_BIO_IMAGE_FACES, Limit.of(1)))
                .thenReturn(Optional.of(task().setResult(JsonConverter.toJson(result))));
        Files.write(file, new byte[]{1, 2, 3});

        assertThat(service.shouldDiscover(artist)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"3000,1200,1280,512", "1200,3000,512,1280", "80,40,80,40"})
    void shouldPreserveAspectRatioWithoutUpscalingOrCropping(int width, int height, int expectedWidth, int expectedHeight) throws Exception {
        BufferedImage original = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(original, "png", file.toFile());

        service.discover(task(), false);

        BufferedImage sent = sentImage();
        assertThat(sent.getWidth()).isEqualTo(expectedWidth);
        assertThat(sent.getHeight()).isEqualTo(expectedHeight);
        assertThat(ImageIO.read(file.toFile()).getWidth()).isEqualTo(width);
        assertThat(ImageIO.read(file.toFile()).getHeight()).isEqualTo(height);
    }

    private BufferedImage sentImage() throws Exception {
        Map<?, ?> request = JsonConverter.mapFromJson(requests.getFirst(), String.class, Object.class);
        List<?> messages = (List<?>) request.get("messages");
        List<?> content = (List<?>) ((Map<?, ?>) messages.getFirst()).get("content");
        String url = content.stream().map(Map.class::cast).filter(item -> item.containsKey("image_url"))
                .map(item -> (String) ((Map<?, ?>) item.get("image_url")).get("url")).findFirst().orElseThrow();
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(url.substring(url.indexOf(',') + 1))));
    }

    @ParameterizedTest
    @CsvSource({"1,80,40,255,0,0", "2,80,40,0,255,0", "3,80,40,255,255,0", "4,80,40,0,0,255",
            "5,40,80,255,0,0", "6,40,80,0,0,255", "7,40,80,255,255,0", "8,40,80,0,255,0"})
    void shouldApplyExifRotationAndMirroringWithoutChangingOriginal(int orientation, int width, int height,
                                                                  int red, int green, int blue) throws Exception {
        byte[] original = orientedJpeg(orientation);
        Files.write(file, original);

        service.discover(task(), false);

        BufferedImage sent = sentImage();
        assertThat(sent.getWidth()).isEqualTo(width);
        assertThat(sent.getHeight()).isEqualTo(height);
        Color corner = new Color(sent.getRGB(10, 10));
        assertThat(corner.getRed()).isCloseTo(red, within(15));
        assertThat(corner.getGreen()).isCloseTo(green, within(15));
        assertThat(corner.getBlue()).isCloseTo(blue, within(15));
        assertThat(Files.readAllBytes(file)).isEqualTo(original);
    }

    private byte[] orientedJpeg(int orientation) throws Exception {
        BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, 40, 20);
            graphics.setColor(Color.GREEN);
            graphics.fillRect(40, 0, 40, 20);
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 20, 40, 20);
            graphics.setColor(Color.YELLOW);
            graphics.fillRect(40, 20, 40, 20);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpeg", output);
            byte[] jpeg = output.toByteArray();
            // APP1 contains a little-endian TIFF header and one IFD0 orientation entry.
            byte[] exif = ByteBuffer.allocate(36).putShort((short) 0xffe1).putShort((short) 34)
                    .put(new byte[]{'E', 'x', 'i', 'f', 0, 0}).order(ByteOrder.LITTLE_ENDIAN)
                    .putShort((short) 0x4949).putShort((short) 42).putInt(8).putShort((short) 1)
                    .putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation)
                    .putShort((short) 0).putInt(0).array();
            output.reset();
            output.write(jpeg, 0, 2);
            output.write(exif);
            output.write(jpeg, 2, jpeg.length - 2);
            return output.toByteArray();
        }
    }

    @Test
    void shouldNotCallModelAfterCancellation() {
        jobSynchronizer.cancelDiscovery();

        assertThatThrownBy(() -> service.discover(task(), true)).isInstanceOf(DiscoveryInterruptedException.class);

        assertThat(requests).isEmpty();
        assertThat(cache).isEmpty();
    }
}
