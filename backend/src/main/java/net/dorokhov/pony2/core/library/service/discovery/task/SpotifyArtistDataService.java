package net.dorokhov.pony2.core.library.service.discovery.task;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.hash.Hashing;
import jakarta.annotation.Nullable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.DiscoveryCancellationMonitor;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryAdvisor;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

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

    private final ChatClient chatClient;
    private final LlmCacheService cacheService;
    private final Validator validator;
    private final ArtistRepository artistRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final LogService logService;
    private final ShutdownService shutdownService;
    private final DiscoveryAdvisor discoveryAdvisor;
    private final DiscoveryCancellationMonitor cancellationMonitor;
    private final TransactionTemplate transactionTemplate;

    private final String systemPrompt;

    public SpotifyArtistDataService(
            ChatClient chatClient,
            LlmCacheService cacheService,
            Validator validator,
            ArtistRepository artistRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            LogService logService,
            ShutdownService shutdownService,
            DiscoveryAdvisor discoveryAdvisor,
            DiscoveryCancellationMonitor cancellationMonitor,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/spotify-artist-data.txt") Resource promptResource
    ) throws IOException {
        this.chatClient = chatClient;
        this.cacheService = cacheService;
        this.validator = validator;
        this.artistRepository = artistRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.logService = logService;
        this.shutdownService = shutdownService;
        this.discoveryAdvisor = discoveryAdvisor;
        this.cancellationMonitor = cancellationMonitor;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        systemPrompt = promptResource.getContentAsString(UTF_8) + "\n"
                + new BeanOutputConverter<>(SpotifyArtistData.class).getFormat();
    }

    public Optional<SpotifyArtistData> discover(DiscoveryTask task, boolean cacheEnabled) {
        cancellationMonitor.taskStarted();
        try {
            return doDiscover(task, cacheEnabled);
        } finally {
            cancellationMonitor.taskFinished();
        }
    }

    private Optional<SpotifyArtistData> doDiscover(DiscoveryTask task, boolean cacheEnabled) {
        cancellationMonitor.interruptIfCancelled();
        if (shutdownService.isShutdown()) {
            throw new DiscoveryInterruptedException();
        }
        String artistId = JsonConverter.fromJson(task.getParameter(), DiscoveryTask.ArtistParameter.class).artistId();
        Request request = transactionTemplate.execute(status -> prepareRequest(artistId));
        if (request == null) {
            return Optional.empty();
        }
        saveRawExchange(task, request, null);
        Artist artist = artistRepository.findById(artistId).orElseThrow();
        String key = "SPOTIFY_ARTIST_DATA:" + Hashing.sha256().hashString(JsonConverter.toJson(request), UTF_8);
        if (cacheEnabled) {
            Optional<String> cached = cacheService.get(SPOTIFY, key, CACHE_VERSION);
            if (cached.isPresent()) {
                logger.debug("Spotify discovery cache hit for artist '{} -> {}'. Cache version: {}.",
                        artist.getId(), artist.getName(), CACHE_VERSION);
                CacheEntry entry = JsonConverter.fromJson(cached.get(), CacheEntry.class);
                saveRawExchange(task, entry.request(), entry.response());
                SpotifyArtistData result = JsonConverter.fromJson(entry.response(), SpotifyArtistData.class);
                logResult(artist, result, "cache");
                return Optional.of(result);
            }
        }
        logger.debug("Requesting Spotify data from LLM for artist '{} -> {}'.\n\n{}\n\n",
                artist.getId(), artist.getName(), request);
        Stopwatch stopwatch = Stopwatch.createStarted();
        String response = chatClient.prompt()
                .advisors(discoveryAdvisor)
                .messages(new SystemMessage(request.systemPrompt()), new UserMessage(request.userPrompt()))
                .call()
                .content();
        cancellationMonitor.interruptIfCancelled();
        saveRawExchange(task, request, response);
        logLlmExchange(artist, request, response, stopwatch);
        SpotifyArtistData result = response != null ? JsonConverter.fromJson(response, SpotifyArtistData.class) : null;
        validateResponse(result, request);
        logResult(artist, result, "llm");
        if (cacheEnabled) {
            cacheService.put(SPOTIFY, key, CACHE_VERSION, JsonConverter.toJson(new CacheEntry(request, response)));
            logger.debug("Cached Spotify discovery response for artist '{} -> {}'. Cache version: {}.",
                    artist.getId(), artist.getName(), CACHE_VERSION);
        }
        return Optional.of(result);
    }

    private void saveRawExchange(DiscoveryTask task, Request request, @Nullable String response) {
        transactionTemplate.executeWithoutResult(status -> discoveryTaskRepository.save(task
                .setRawRequest(JsonConverter.toJson(request))
                .setRawResult(response)));
    }

    private void logLlmExchange(Artist artist, Request request, @Nullable String response, Stopwatch stopwatch) {
        if (logger.isDebugEnabled()) {
            logger.debug("\n\nSpotify LLM exchange for artist '{} -> {}' completed in {} ms.\n\nRequest:\n{}\n\nResponse:\n{}\n\n",
                    artist.getId(), artist.getName(), stopwatch.elapsed(), JsonConverter.toPrettyJson(request), formatResponseForLog(response));
        }
    }

    private String formatResponseForLog(@Nullable String response) {
        try {
            return JsonConverter.toPrettyJson(JsonConverter.fromJson(response));
        } catch (RuntimeException e) {
            return "Invalid JSON:\n" + response;
        }
    }

    private void logResult(Artist artist, SpotifyArtistData result, String source) {
        logger.debug("Spotify discovery result for artist '{} -> {}'. Source: {}, result: {}.",
                artist.getId(), artist.getName(), source, result);
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
    ) {
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("systemPrompt", systemPrompt)
                    .add("userPrompt", userPrompt)
                    .add("albumTitles", albumTitles)
                    .toString();
        }
    }

    private record CacheEntry(
            Request request,
            String response
    ) {}
}
