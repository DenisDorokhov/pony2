package net.dorokhov.pony2.core.library.service.discovery.spotify;

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
import net.dorokhov.pony2.common.TextTemplate;
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

    private final TextTemplate identitySystemPrompt;
    private final TextTemplate identityUserPrompt;
    private final TextTemplate popularTracksSystemPrompt;
    private final TextTemplate popularTracksUserPrompt;
    private final TextTemplate biographySystemPrompt;
    private final TextTemplate biographyUserPrompt;
    private final TextTemplate aboutMediaSystemPrompt;
    private final TextTemplate aboutMediaUserPrompt;
    private final TextTemplate similarArtistsSystemPrompt;
    private final TextTemplate similarArtistsUserPrompt;
    private final TextTemplate identityVerificationUserPrompt;
    private final TextTemplate popularTracksVerificationUserPrompt;
    private final TextTemplate biographyVerificationUserPrompt;
    private final TextTemplate aboutMediaVerificationUserPrompt;
    private final TextTemplate similarArtistsVerificationUserPrompt;

    public SpotifyArtistDataService(
            DiscoveryTaskLlmExecutor llmOperation,
            Validator validator,
            ArtistRepository artistRepository,
            LogService logService,
            LibraryJobSynchronizer jobSynchronizer,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/spotify-artist-data-identity-system.vm")
            Resource identitySystemPrompt,
            @Value("classpath:prompts/spotify-artist-data-identity-user.vm")
            Resource identityUserPrompt,
            @Value("classpath:prompts/spotify-artist-data-popular-tracks-system.vm")
            Resource popularTracksSystemPrompt,
            @Value("classpath:prompts/spotify-artist-data-popular-tracks-user.vm")
            Resource popularTracksUserPrompt,
            @Value("classpath:prompts/spotify-artist-data-biography-system.vm")
            Resource biographySystemPrompt,
            @Value("classpath:prompts/spotify-artist-data-biography-user.vm")
            Resource biographyUserPrompt,
            @Value("classpath:prompts/spotify-artist-data-about-media-system.vm")
            Resource aboutMediaSystemPrompt,
            @Value("classpath:prompts/spotify-artist-data-about-media-user.vm")
            Resource aboutMediaUserPrompt,
            @Value("classpath:prompts/spotify-artist-data-similar-artists-system.vm")
            Resource similarArtistsSystemPrompt,
            @Value("classpath:prompts/spotify-artist-data-similar-artists-user.vm")
            Resource similarArtistsUserPrompt,
            @Value("classpath:prompts/spotify-artist-data-identity-verify-user.vm")
            Resource identityVerificationUserPrompt,
            @Value("classpath:prompts/spotify-artist-data-popular-tracks-verify-user.vm")
            Resource popularTracksVerificationUserPrompt,
            @Value("classpath:prompts/spotify-artist-data-biography-verify-user.vm")
            Resource biographyVerificationUserPrompt,
            @Value("classpath:prompts/spotify-artist-data-about-media-verify-user.vm")
            Resource aboutMediaVerificationUserPrompt,
            @Value("classpath:prompts/spotify-artist-data-similar-artists-verify-user.vm")
            Resource similarArtistsVerificationUserPrompt
    ) throws IOException {
        this.llmOperation = llmOperation;
        this.validator = validator;
        this.artistRepository = artistRepository;
        this.logService = logService;
        this.jobSynchronizer = jobSynchronizer;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        this.identitySystemPrompt = new TextTemplate(identitySystemPrompt);
        this.identityUserPrompt = new TextTemplate(identityUserPrompt);
        this.popularTracksSystemPrompt = new TextTemplate(popularTracksSystemPrompt);
        this.popularTracksUserPrompt = new TextTemplate(popularTracksUserPrompt);
        this.biographySystemPrompt = new TextTemplate(biographySystemPrompt);
        this.biographyUserPrompt = new TextTemplate(biographyUserPrompt);
        this.aboutMediaSystemPrompt = new TextTemplate(aboutMediaSystemPrompt);
        this.aboutMediaUserPrompt = new TextTemplate(aboutMediaUserPrompt);
        this.similarArtistsSystemPrompt = new TextTemplate(similarArtistsSystemPrompt);
        this.similarArtistsUserPrompt = new TextTemplate(similarArtistsUserPrompt);
        this.identityVerificationUserPrompt = new TextTemplate(identityVerificationUserPrompt);
        this.popularTracksVerificationUserPrompt = new TextTemplate(popularTracksVerificationUserPrompt);
        this.biographyVerificationUserPrompt = new TextTemplate(biographyVerificationUserPrompt);
        this.aboutMediaVerificationUserPrompt = new TextTemplate(aboutMediaVerificationUserPrompt);
        this.similarArtistsVerificationUserPrompt = new TextTemplate(similarArtistsVerificationUserPrompt);
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
                "SPOTIFY_ARTIST_IDENTITY", cacheEnabled, identityVerificationUserPrompt, response -> validateIdentity(response, request));
        if (identity.status() != Status.FOUND) {
            return null;
        }
        SpotifyArtist artist = requireNonNull(identity.artist());
        String artistJson = JsonConverter.toJson(artist);
        TopTracksResponse topTracks = callStage(task, artistId,
                new ArtistRequest(popularTracksSystemPrompt.render(), popularTracksUserPrompt.render(Map.of("artist", artistJson))),
                TopTracksResponse.class, "SPOTIFY_ARTIST_POPULAR_TRACKS", cacheEnabled, popularTracksVerificationUserPrompt, this::validateTopTracks);
        BiographyResponse biography = callStage(task, artistId,
                new ArtistRequest(biographySystemPrompt.render(), biographyUserPrompt.render(Map.of("artist", artistJson))),
                BiographyResponse.class, "SPOTIFY_ARTIST_BIOGRAPHY", cacheEnabled, biographyVerificationUserPrompt, response -> {});
        AboutMediaResponse aboutMedia = callStage(task, artistId,
                new ArtistRequest(aboutMediaSystemPrompt.render(), aboutMediaUserPrompt.render(Map.of("artist", artistJson))),
                AboutMediaResponse.class, "SPOTIFY_ARTIST_ABOUT_MEDIA", cacheEnabled, aboutMediaVerificationUserPrompt, response -> {});
        SimilarArtistsResponse similarArtists = callStage(task, artistId,
                new ArtistRequest(similarArtistsSystemPrompt.render(), similarArtistsUserPrompt.render(Map.of("artist", artistJson))),
                SimilarArtistsResponse.class, "SPOTIFY_ARTIST_SIMILAR_ARTISTS", cacheEnabled, similarArtistsVerificationUserPrompt,
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
        List<Map<String, Object>> albumData = albums.stream()
                .map(album -> Map.<String, Object>of("title", JsonConverter.toJson(album.getName()),
                        "hasYear", album.getYear() != null, "year", JsonConverter.toJson(album.getYear())))
                .toList();
        String userPrompt = identityUserPrompt.render(Map.of("artistName", JsonConverter.toJson(artist.getName()), "albums", albumData));
        return new Request(identitySystemPrompt.render(), userPrompt, albums.stream().map(Album::getName).toList());
    }

    private <R> R callStage(
            DiscoveryTask task, String artistId, DiscoveryTaskLlmExecutor.Request request, Class<R> responseClass,
            String cachePrefix, boolean cacheEnabled, TextTemplate verificationUserPrompt, Consumer<R> responseValidator
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
                throw new LlmFollowUpException(verificationUserPrompt.render(Map.of("validationError", JsonConverter.toJson(e.getMessage()))), e);
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

    private record TopTracksResponse(@Nullable List<@NotNull @Valid TopTrack> topTracks) {}

    private record BiographyResponse(@Nullable String biography) {}

    private record AboutMediaResponse(
            @Nullable List<@NotNull @Valid ExternalLink> links,
            @Nullable String imageUrl
    ) {}

    private record SimilarArtistsResponse(@Nullable List<@NotNull @Valid SpotifyArtist> similarArtists) {}

    private enum Status {
        FOUND, NOT_FOUND, AMBIGUOUS
    }

    private record Request(
            String systemPrompt,
            String userPrompt,
            List<String> albumTitles
    ) implements DiscoveryTaskLlmExecutor.Request {

        @Override
        public RawRequest rawRequest() {
            return new RawRequest(systemPrompt, userPrompt, albumTitles);
        }

        @Override
        public CacheKey cacheKey() {
            return new CacheKey(systemPrompt, userPrompt, albumTitles);
        }

        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        }
    }

    private record RawRequest(String systemPrompt, String userPrompt, List<String> albumTitles) {}

    private record CacheKey(String systemPrompt, String userPrompt, List<String> albumTitles) {}

    private record ArtistRequest(String systemPrompt, String userPrompt) implements DiscoveryTaskLlmExecutor.Request {

        @Override
        public ArtistRawRequest rawRequest() {
            return new ArtistRawRequest(systemPrompt, userPrompt);
        }

        @Override
        public ArtistCacheKey cacheKey() {
            return new ArtistCacheKey(systemPrompt, userPrompt);
        }

        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        }
    }

    private record ArtistRawRequest(String systemPrompt, String userPrompt) {}

    private record ArtistCacheKey(String systemPrompt, String userPrompt) {}
}
