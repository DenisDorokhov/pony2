package net.dorokhov.pony2.core.library.service.discovery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import jakarta.annotation.Nullable;
import net.coobird.thumbnailator.Thumbnails;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmJsonConverter;
import net.dorokhov.pony2.core.library.repository.ArtistDiscoveryRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.exception.LlmFollowUpException;
import net.dorokhov.pony2.core.library.service.image.ExternalImageStorage;
import net.dorokhov.pony2.core.library.service.image.ImageSizeReader;
import net.dorokhov.pony2.core.library.service.image.domain.ImageSize;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.core.JacksonException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion.SPOTIFY;
import static net.dorokhov.pony2.api.library.domain.DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES;

@Service
public class ArtistBioImageFacesService {

    private static final int CACHE_VERSION = 1;

    private final DiscoveryTaskLlmExecutor llmExecutor;
    private final ArtistDiscoveryRepository artistDiscoveryRepository;
    private final DiscoveryTaskRepository taskRepository;
    private final ExternalImageStorage imageStorage;
    private final ImageSizeReader imageSizeReader;
    private final ConfigService configService;
    private final TransactionTemplate transactionTemplate;
    private final String prompt;

    public ArtistBioImageFacesService(
            DiscoveryTaskLlmExecutor llmExecutor,
            ArtistDiscoveryRepository artistDiscoveryRepository,
            DiscoveryTaskRepository taskRepository,
            ExternalImageStorage imageStorage,
            ImageSizeReader imageSizeReader,
            ConfigService configService,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/artist-bio-image-faces.txt") Resource promptResource
    ) throws IOException {
        this.llmExecutor = llmExecutor;
        this.artistDiscoveryRepository = artistDiscoveryRepository;
        this.taskRepository = taskRepository;
        this.imageStorage = imageStorage;
        this.imageSizeReader = imageSizeReader;
        this.configService = configService;
        transactionTemplate = new TransactionTemplate(transactionManager);
        prompt = promptResource.getContentAsString(UTF_8);
    }

    public boolean shouldDiscover(Artist artist) {
        DiscoveryTask completed = taskRepository.findLatestCompletedByArtistIdAndType(
                artist.getId(), ARTIST_BIO_IMAGE_FACES, Limit.of(1)).orElse(null);
        if (completed == null) {
            return true;
        }
        String previousResult = completed.getResult();
        ExternalImageFile image = findBioImageFile(artist.getId());
        if (image == null) {
            return previousResult != null;
        }
        if (previousResult == null) {
            return true;
        }
        Result previous = JsonConverter.fromJson(previousResult, Result.class);
        if (!image.getExternalImage().getId().equals(previous.externalImageId())) {
            return true;
        }
        try {
            return !createRequest(imageHash(image), new byte[0]).analysisKey().equals(previous.analysisKey());
        } catch (UncheckedIOException e) {
            // Let task execution record the image read failure without aborting the entire job.
            return true;
        }
    }

    private @Nullable ExternalImageFile findBioImageFile(String artistId) {
        return transactionTemplate.execute(status -> artistDiscoveryRepository.findByArtistId(artistId)
                .map(ArtistDiscovery::getBioExternalImage)
                .flatMap(image -> imageStorage.getImageFile(image.getId()))
                .orElse(null));
    }

    private String imageHash(ExternalImageFile image) {
        try {
            return Files.asByteSource(image.getFile()).hash(Hashing.sha256()).toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Request createRequest(String imageHash, byte[] prepared) {
        ConfigSet config = configService.get();
        return new Request(prompt, config.llmUrl(), config.llmModel(), imageHash, CACHE_VERSION, prepared);
    }

    public @Nullable Result discover(DiscoveryTask task, boolean cacheEnabled) {
        ExternalImageFile image = findBioImageFile(task.getArtist().getId());
        if (image == null) {
            return null;
        }
        Request request = createRequest(imageHash(image), prepareImage(image));
        List<ImageFace> faces = llmExecutor.call(task, request, Response.class,
                cacheEnabled ? new DiscoveryTaskLlmExecutor.CacheSettings(SPOTIFY, "artistBioImageFaces", CACHE_VERSION) : null,
                this::parseFaces);
        return new Result(image.getExternalImage().getId(), request.analysisKey(), faces);
    }

    private byte[] prepareImage(ExternalImageFile image) {
        try (InputStream input = new FileInputStream(image.getFile());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageSize size = imageSizeReader.read(image.getFile());
            Thumbnails.of(input).scale(Math.min(1.0, 1280.0 / Math.max(size.getWidth(), size.getHeight())))
                    .useExifOrientation(true).outputFormat("png").toOutputStream(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<ImageFace> parseFaces(@Nullable String json) {
        try {
            if (json == null || json.isBlank()) {
                throw new IllegalArgumentException("The response must not be empty.");
            }
            Response response = LlmJsonConverter.fromJson(json, Response.class);
            if (response == null || response.faces() == null) {
                throw new IllegalArgumentException("The faces array is required.");
            }
            List<ImageFace> faces = new ArrayList<>();
            for (List<Double> box : response.faces()) {
                if (box == null || box.size() != 4 || box.stream().anyMatch(value ->
                        value == null || !Double.isFinite(value) || value < 0 || value > 1000)) {
                    throw new IllegalArgumentException("Each face must contain four finite coordinates between 0 and 1000.");
                }
                double xMin = box.get(0);
                double yMin = box.get(1);
                double xMax = box.get(2);
                double yMax = box.get(3);
                if (xMin >= xMax || yMin >= yMax) {
                    throw new IllegalArgumentException("Each face must have x_min < x_max and y_min < y_max.");
                }
                faces.add(new ImageFace(xMin / 1000, yMin / 1000, xMax / 1000, yMax / 1000));
            }
            return List.copyOf(faces);
        } catch (JacksonException | IllegalArgumentException e) {
            throw new LlmFollowUpException("Invalid face detection response: " + e.getMessage()
                    + " Return the complete corrected JSON object. Return an empty faces array only if no faces are visible.", e);
        }
    }

    public record Result(@Nullable String externalImageId, @Nullable String analysisKey, List<ImageFace> faces) {
    }

    private record Response(List<List<Double>> faces) {
    }

    private record Request(
            String prompt,
            @Nullable String endpoint,
            @Nullable String model,
            String imageHash,
            int version,
            @JsonIgnore byte[] image
    ) implements DiscoveryTaskLlmExecutor.Request {

        private String analysisKey() {
            return Hashing.sha256().hashString(JsonConverter.toJson(this), UTF_8).toString();
        }

        @Override
        public Prompt toPrompt() {
            UserMessage message = UserMessage.builder().text(prompt)
                    .media(new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(image))).build();
            return new Prompt(
                    List.of(message),
                    OpenAiChatOptions.builder()
                            .model(model)
                            .seed(42)
                            .maxTokens(1024)
                            .extraBody(Map.of("chat_template_kwargs", Map.of("enable_thinking", false)))
                            .build());
        }
    }
}
