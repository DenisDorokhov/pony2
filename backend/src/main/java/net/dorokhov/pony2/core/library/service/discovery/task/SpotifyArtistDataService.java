package net.dorokhov.pony2.core.library.service.discovery.task;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Nullable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmJsonConverter;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryTaskLlmExecutor;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.library.service.exception.FollowUpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.dorokhov.pony2.api.library.domain.SpotifyArtistData.Status.FOUND;
import static net.dorokhov.pony2.api.llm.domain.LlmCacheRegion.SPOTIFY;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class SpotifyArtistDataService {

    private static final int CACHE_VERSION = 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryTaskLlmExecutor llmOperation;
    private final Validator validator;
    private final ArtistRepository artistRepository;
    private final LogService logService;
    private final ShutdownService shutdownService;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final TransactionTemplate transactionTemplate;

    private final String systemPrompt;
    private final String verificationPrompt;

    public SpotifyArtistDataService(
            DiscoveryTaskLlmExecutor llmOperation,
            Validator validator,
            ArtistRepository artistRepository,
            LogService logService,
            ShutdownService shutdownService,
            LibraryJobSynchronizer jobSynchronizer,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/spotify-artist-data.txt") Resource promptResource,
            @Value("classpath:prompts/spotify-artist-data-verification.txt") Resource verificationPromptResource
    ) throws IOException {
        this.llmOperation = llmOperation;
        this.validator = validator;
        this.artistRepository = artistRepository;
        this.logService = logService;
        this.shutdownService = shutdownService;
        this.jobSynchronizer = jobSynchronizer;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        systemPrompt = promptResource.getContentAsString(UTF_8) + "\n"
                + new BeanOutputConverter<>(SpotifyArtistData.class).getFormat();
        verificationPrompt = verificationPromptResource.getContentAsString(UTF_8);
    }

    public Optional<SpotifyArtistData> discover(DiscoveryTask task, boolean cacheEnabled) {
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignored = jobSynchronizer.registerDiscoveryTask()) {
            return doDiscover(task, cacheEnabled);
        }
    }

    private Optional<SpotifyArtistData> doDiscover(DiscoveryTask task, boolean cacheEnabled) {
        jobSynchronizer.interruptDiscoveryIfCancelled();
        if (shutdownService.isShutdown()) {
            throw new DiscoveryInterruptedException();
        }
        String artistId = JsonConverter.fromJson(task.getParameter(), DiscoveryTask.ArtistParameter.class).artistId();
        Request request = transactionTemplate.execute(status -> prepareRequest(artistId));
        if (request == null) {
            return Optional.empty();
        }
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled ? new DiscoveryTaskLlmExecutor.CacheSettings(
                SPOTIFY, "SPOTIFY_ARTIST_DATA", CACHE_VERSION) : null;
        SpotifyArtistData result = llmOperation.call(task, request, cacheSettings, response -> {
            try {
                SpotifyArtistData data = response != null ? LlmJsonConverter.fromJson(response, SpotifyArtistData.class) : null;
                validateResponse(data, request);
                return data;
            } catch (RuntimeException e) {
                if (!(e instanceof IllegalStateException) && !(e instanceof JacksonException)) {
                    throw e;
                }
                logService.warn(logger, "Invalid Spotify response from the LLM for artist '{}': {}", artistId, e.getMessage());
                throw new FollowUpException(verificationPrompt.formatted(JsonConverter.toJson(e.getMessage())), e);
            }
        });
        logger.debug("Spotify discovery result for artist '{}': {}.", artistId, result);
        return Optional.of(result);
    }

    @Nullable
    private Request prepareRequest(String artistId) {
        Artist artist = artistRepository.findById(artistId).orElseThrow();
        List<Album> albums = artist.getAlbums().stream().sorted().toList();
        if (albums.isEmpty() || (albums.size() == 1 && albums.getFirst().getName() == null)) {
            logService.info(logger, "Skipping Spotify discovery for artist '{} -> {}': no album title is available to identify the artist.",
                    artist.getId(), artist.getName());
            return null;
        }
        if (artist.getName() == null) {
            logService.info(logger, "Skipping Spotify discovery for artist '{} -> {}': the artist name is unknown.",
                    artist.getId(), artist.getName());
            return null;
        }
        String albumList = albums.stream()
                .map(album -> "- " + JsonConverter.toJson(album.getName())
                        + (album.getYear() != null ? " (year: " + album.getYear() + ")" : ""))
                .collect(Collectors.joining("\n"));
        String userPrompt = "Artist name: " + JsonConverter.toJson(artist.getName()) + "\nAlbums from the database:\n" + albumList;
        return new Request(systemPrompt, userPrompt, albums.stream().map(Album::getName).toList());
    }

    private void validateResponse(@Nullable SpotifyArtistData response, Request request) {
        checkState(response != null, "The LLM returned an invalid Spotify response: the response is null.");
        Set<ConstraintViolation<SpotifyArtistData>> violations = validator.validate(response);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("The LLM returned an invalid Spotify response: validation failed: "
                    + violations.stream()
                            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                            .sorted()
                            .collect(Collectors.joining("; ")) + ".");
        }
        if (response.status() != FOUND) {
            return;
        }
        checkState(response.artist() != null && response.matchedAlbum() != null,
                "The LLM returned an invalid Spotify response: FOUND requires an artist and a matched album. Artist present: %s, matched album present: %s.",
                response.artist() != null, response.matchedAlbum() != null);
        checkState(request.albumTitles().contains(response.matchedAlbum().inputTitle()),
                "The LLM returned an invalid Spotify response: matched input album '%s' was not supplied. Album titles: %s.",
                response.matchedAlbum().inputTitle(), request.albumTitles());
    }

    @JsonPropertyOrder({"systemPrompt", "userPrompt", "albumTitles"})
    private record Request(
            String systemPrompt,
            String userPrompt,
            List<String> albumTitles
    ) implements DiscoveryTaskLlmExecutor.Request {
        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        }
    }
}
