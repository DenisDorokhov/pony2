package net.dorokhov.pony2.core.library.service.discovery.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData.ExternalLink;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData.MatchedAlbum;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData.SpotifyArtist;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData.TopTrack;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmJsonConverter;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryTaskLlmExecutor;
import net.dorokhov.pony2.core.library.service.exception.LlmFollowUpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion.SPOTIFY;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class SpotifyArtistDataService {

    private static final int CACHE_VERSION = 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryTaskLlmExecutor llmOperation;
    private final Validator validator;
    private final ArtistRepository artistRepository;
    private final LogService logService;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final TransactionTemplate transactionTemplate;

    private final String identityPrompt;
    private final String verificationPrompt;
    private final String popularTracksPrompt;
    private final String biographyPrompt;
    private final String aboutMediaPrompt;
    private final String similarArtistsPrompt;

    public SpotifyArtistDataService(
            DiscoveryTaskLlmExecutor llmOperation,
            Validator validator,
            ArtistRepository artistRepository,
            LogService logService,
            LibraryJobSynchronizer jobSynchronizer,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/spotify-artist-identity.txt") Resource identityPromptResource,
            @Value("classpath:prompts/spotify-artist-stage-verification.txt") Resource verificationPromptResource,
            @Value("classpath:prompts/spotify-artist-popular-tracks.txt") Resource popularTracksPromptResource,
            @Value("classpath:prompts/spotify-artist-biography.txt") Resource biographyPromptResource,
            @Value("classpath:prompts/spotify-artist-about-media.txt") Resource aboutMediaPromptResource,
            @Value("classpath:prompts/spotify-artist-similar-artists.txt") Resource similarArtistsPromptResource
    ) throws IOException {
        this.llmOperation = llmOperation;
        this.validator = validator;
        this.artistRepository = artistRepository;
        this.logService = logService;
        this.jobSynchronizer = jobSynchronizer;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        identityPrompt = identityPromptResource.getContentAsString(UTF_8);
        verificationPrompt = verificationPromptResource.getContentAsString(UTF_8);
        popularTracksPrompt = popularTracksPromptResource.getContentAsString(UTF_8);
        biographyPrompt = biographyPromptResource.getContentAsString(UTF_8);
        aboutMediaPrompt = aboutMediaPromptResource.getContentAsString(UTF_8);
        similarArtistsPrompt = similarArtistsPromptResource.getContentAsString(UTF_8);
    }

    public @Nullable SpotifyArtistData discover(DiscoveryTask task, boolean cacheEnabled) {
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignored = jobSynchronizer.registerDiscoveryTask()) {
            return doDiscover(task, cacheEnabled);
        }
    }

    private @Nullable SpotifyArtistData doDiscover(DiscoveryTask task, boolean cacheEnabled) {
        jobSynchronizer.interruptDiscoveryIfStopped();
        String artistId = requireNonNull(task.getArtist(),
                () -> "Discovery task '" + task.getId() + "' has no associated artist.").getId();
        Request request = transactionTemplate.execute(status -> prepareRequest(artistId));
        if (request == null) {
            return null;
        }
        IdentityResponse identity = callStage(task, artistId, request, IdentityResponse.class,
                "SPOTIFY_ARTIST_IDENTITY", cacheEnabled, response -> validateIdentity(response, request));
        if (identity.status() != Status.FOUND) {
            return null;
        }
        SpotifyArtist artist = requireNonNull(identity.artist());
        String userPrompt = "Confirmed Spotify artist: " + JsonConverter.toJson(artist);
        TopTracksResponse topTracks = callStage(task, artistId, new ArtistRequest(popularTracksPrompt, userPrompt),
                TopTracksResponse.class, "SPOTIFY_ARTIST_POPULAR_TRACKS", cacheEnabled, this::validateTopTracks);
        BiographyResponse biography = callStage(task, artistId, new ArtistRequest(biographyPrompt, userPrompt),
                BiographyResponse.class, "SPOTIFY_ARTIST_BIOGRAPHY", cacheEnabled, response -> {});
        AboutMediaResponse aboutMedia = callStage(task, artistId, new ArtistRequest(aboutMediaPrompt, userPrompt),
                AboutMediaResponse.class, "SPOTIFY_ARTIST_ABOUT_MEDIA", cacheEnabled, response -> {});
        SimilarArtistsResponse similarArtists = callStage(task, artistId, new ArtistRequest(similarArtistsPrompt, userPrompt),
                SimilarArtistsResponse.class, "SPOTIFY_ARTIST_SIMILAR_ARTISTS", cacheEnabled,
                response -> validateSimilarArtists(response, artist));
        SpotifyArtistData result = new SpotifyArtistData(artist, requireNonNull(identity.matchedAlbum()),
                topTracks.topTracks(), similarArtists.similarArtists(), biography.biography(),
                aboutMedia.links(), aboutMedia.imageUrl(), identity.avatarUrl());
        logger.debug("Spotify discovery result for artist '{}': {}.", artistId, result);
        return result;
    }

    private @Nullable Request prepareRequest(String artistId) {
        Artist artist = artistRepository.findById(artistId).orElseThrow();
        List<Album> albums = artist.getAlbums().stream().sorted().toList();
        if (albums.isEmpty() || (albums.size() == 1 && albums.getFirst().getName() == null)) {
            logger.debug("Skipping Spotify discovery for artist '{}': no album title is available to identify the artist.",
                    artist.getName());
            return null;
        }
        if (artist.getName() == null) {
            logger.debug("Skipping Spotify discovery for artist '{}': the artist name is unknown.",
                    artist.getId());
            return null;
        }
        String albumList = albums.stream()
                .map(album -> "- " + JsonConverter.toJson(album.getName())
                        + (album.getYear() != null ? " (year: " + album.getYear() + ")" : ""))
                .collect(Collectors.joining("\n"));
        String userPrompt = "Artist name: " + JsonConverter.toJson(artist.getName()) + "\nAlbums from the database:\n" + albumList;
        return new Request(identityPrompt, userPrompt, albums.stream().map(Album::getName).toList());
    }

    private <R> R callStage(
            DiscoveryTask task, String artistId, DiscoveryTaskLlmExecutor.Request request, Class<R> responseClass,
            String cachePrefix, boolean cacheEnabled, Consumer<R> responseValidator
    ) {
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(SPOTIFY, cachePrefix, CACHE_VERSION) : null;
        return llmOperation.call(task, request, responseClass, cacheSettings, response -> {
            try {
                R data = response != null ? LlmJsonConverter.fromJson(response, responseClass) : null;
                validateResponse(data);
                responseValidator.accept(data);
                return data;
            } catch (JacksonException | IllegalStateException e) {
                transactionTemplate.executeWithoutResult(status ->
                        logService.warn(logger, "Invalid Spotify response from the LLM for artist '{}'.",
                                artistRepository.findById(artistId).orElseThrow().getName(), e));
                throw new LlmFollowUpException(verificationPrompt.formatted(JsonConverter.toJson(e.getMessage())), e);
            }
        });
    }

    private <R> void validateResponse(@Nullable R response) {
        checkState(response != null, "The LLM returned an invalid Spotify response: the response is null.");
        Set<ConstraintViolation<R>> violations = validator.validate(response);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("The LLM returned an invalid Spotify response: validation failed: "
                    + violations.stream()
                            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                            .sorted()
                            .collect(Collectors.joining("; ")) + ".");
        }
    }

    private void validateIdentity(IdentityResponse response, Request request) {
        if (response.status() != Status.FOUND) {
            return;
        }
        checkState(response.artist() != null && response.matchedAlbum() != null,
                "The LLM returned an invalid Spotify response: FOUND requires an artist and a matched album. Artist present: %s, matched album present: %s.",
                response.artist() != null, response.matchedAlbum() != null);
        checkState(request.albumTitles().contains(response.matchedAlbum().inputTitle()),
                "The LLM returned an invalid Spotify response: matched input album '%s' was not supplied. Album titles: %s.",
                response.matchedAlbum().inputTitle(), request.albumTitles());
    }

    private void validateTopTracks(TopTracksResponse response) {
        if (response.topTracks() == null) {
            return;
        }
        checkState(response.topTracks().size() <= 10,
                "The LLM returned an invalid Spotify response: topTracks contains more than 10 tracks.");
        checkState(response.topTracks().stream().map(TopTrack::id).distinct().count() == response.topTracks().size(),
                "The LLM returned an invalid Spotify response: topTracks contains duplicate track IDs.");
    }

    private void validateSimilarArtists(SimilarArtistsResponse response, SpotifyArtist artist) {
        if (response.similarArtists() == null) {
            return;
        }
        checkState(response.similarArtists().stream().map(SpotifyArtist::id).distinct().count() == response.similarArtists().size(),
                "The LLM returned an invalid Spotify response: similarArtists contains duplicate artist IDs.");
        checkState(response.similarArtists().stream().noneMatch(similarArtist -> similarArtist.id().equals(artist.id())),
                "The LLM returned an invalid Spotify response: similarArtists contains the selected artist.");
    }

    private record IdentityResponse(
            @NotNull Status status,
            @Nullable @Valid SpotifyArtist artist,
            @Nullable @Valid MatchedAlbum matchedAlbum,
            @Nullable String avatarUrl
    ) {}

    private record TopTracksResponse(@JsonProperty(required = true) @Nullable List<@NotNull @Valid TopTrack> topTracks) {}

    private record BiographyResponse(@JsonProperty(required = true) @Nullable String biography) {}

    private record AboutMediaResponse(
            @JsonProperty(required = true) @Nullable List<@NotNull @Valid ExternalLink> links,
            @JsonProperty(required = true) @Nullable String imageUrl
    ) {}

    private record SimilarArtistsResponse(@JsonProperty(required = true) @Nullable List<@NotNull @Valid SpotifyArtist> similarArtists) {}

    private enum Status {
        FOUND, NOT_FOUND, AMBIGUOUS
    }

    private record Request(
            String systemPrompt,
            String userPrompt,
            List<String> albumTitles
    ) implements DiscoveryTaskLlmExecutor.Request {
        @Override
        public String toCacheJson() {
            return JsonConverter.toJson(Map.of("systemPrompt", systemPrompt, "userPrompt", userPrompt, "albumTitles", albumTitles));
        }

        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        }
    }

    private record ArtistRequest(String systemPrompt, String userPrompt) implements DiscoveryTaskLlmExecutor.Request {
        @Override
        public String toCacheJson() {
            return JsonConverter.toJson(Map.of("systemPrompt", systemPrompt, "userPrompt", userPrompt));
        }

        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        }
    }
}
