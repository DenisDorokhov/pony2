package net.dorokhov.pony2.core.library.service.discovery;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.hash.Hashing;
import jakarta.annotation.Nullable;
import jakarta.validation.Validator;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
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
    private final LogService logService;
    private final ShutdownService shutdownService;
    private final TransactionTemplate transactionTemplate;

    private final String systemPrompt;

    public SpotifyArtistDataService(
            ChatClient chatClient,
            LlmCacheService cacheService,
            Validator validator,
            ArtistRepository artistRepository,
            LogService logService,
            ShutdownService shutdownService,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/spotify-artist-data.txt") Resource promptResource
    ) throws IOException {
        this.chatClient = chatClient;
        this.cacheService = cacheService;
        this.validator = validator;
        this.artistRepository = artistRepository;
        this.logService = logService;
        this.shutdownService = shutdownService;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        systemPrompt = promptResource.getContentAsString(UTF_8) + "\n"
                + new BeanOutputConverter<>(SpotifyArtistData.class).getFormat();
    }

    public Optional<SpotifyArtistData> discover(String artistId) {
        if (shutdownService.isShutdown()) {
            throw new DiscoveryInterruptedException();
        }
        Request request = transactionTemplate.execute(status ->
                createRequest(artistRepository.findById(artistId).orElseThrow()));
        if (request == null) {
            return Optional.empty();
        }
        String key = "SPOTIFY_ARTIST_DATA:" + Hashing.sha256().hashString(JsonConverter.toJson(request), UTF_8);
        Optional<String> cached = cacheService.get(SPOTIFY, key, CACHE_VERSION);
        if (cached.isPresent()) {
            CacheEntry entry = JsonConverter.fromJson(cached.get(), CacheEntry.class);
            return Optional.of(JsonConverter.fromJson(entry.response(), SpotifyArtistData.class));
        }
        String response = chatClient.prompt()
                .messages(new SystemMessage(request.systemPrompt()), new UserMessage(request.userPrompt()))
                .call()
                .content();
        SpotifyArtistData result = JsonConverter.fromJson(response, SpotifyArtistData.class);
        checkState(isValidResponse(result, request), "The LLM returned an invalid Spotify response.");
        cacheService.put(SPOTIFY, key, CACHE_VERSION, JsonConverter.toJson(new CacheEntry(request, response)));
        return Optional.of(result);
    }

    @Nullable
    private Request createRequest(Artist artist) {
        List<Album> albums = artist.getAlbums().stream().sorted().toList();
        if (albums.isEmpty() || (albums.size() == 1 && albums.getFirst().getName() == null)) {
            logService.info(logger, "Skipping Spotify discovery for artist '{}' ({}): no album title is available to identify the artist.",
                    artist.getName(), artist.getId());
            return null;
        }
        if (artist.getName() == null) {
            logService.info(logger, "Skipping Spotify discovery for artist '{}': the artist name is unknown.",
                    artist.getId());
            return null;
        }
        String albumList = albums.stream()
                .map(album -> "- " + JsonConverter.toJson(album.getName())
                        + (album.getYear() != null ? " (year: " + album.getYear() + ")" : ""))
                .collect(Collectors.joining("\n"));
        String userPrompt = "Artist name: " + JsonConverter.toJson(artist.getName()) + "\nAlbums from the database:\n" + albumList;
        return new Request(
                systemPrompt, userPrompt,
                albums.stream().map(Album::getName).toList()
        );
    }

    private boolean isValidResponse(SpotifyArtistData response, Request request) {
        if (response == null || !validator.validate(response).isEmpty()) {
            return false;
        }
        if (response.status() != FOUND) {
            return true;
        }
        return response.artist() != null && response.matchedAlbum() != null &&
                request.albumTitles().contains(response.matchedAlbum().inputTitle());
    }

    @JsonPropertyOrder({"systemPrompt", "userPrompt", "albumTitles"})
    private record Request(
            String systemPrompt,
            String userPrompt,
            List<String> albumTitles
    ) {}

    private record CacheEntry(
            Request request,
            String response
    ) {}
}
