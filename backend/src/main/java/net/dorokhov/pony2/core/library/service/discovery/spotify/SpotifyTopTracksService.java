package net.dorokhov.pony2.core.library.service.discovery.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData.ReleaseType;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmJsonConverter;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryTaskLlmExecutor;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryTaskResultReader;
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
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion.SPOTIFY;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class SpotifyTopTracksService {

    private static final int CACHE_VERSION = 1;

    private static final JsonMapper RESPONSE_MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscoveryTaskLlmExecutor llmOperation;
    private final ArtistRepository artistRepository;
    private final DiscoveryTaskResultReader resultReader;
    private final LibraryService libraryService;
    private final LogService logService;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final TransactionTemplate transactionTemplate;
    private final String albumsPrompt;
    private final String tracksPrompt;
    private final String verificationPrompt;
    private final String fallbackPrompt;
    private final String fallbackVerificationPrompt;

    public SpotifyTopTracksService(
            DiscoveryTaskLlmExecutor llmOperation,
            ArtistRepository artistRepository,
            DiscoveryTaskResultReader resultReader,
            LibraryService libraryService,
            LogService logService,
            LibraryJobSynchronizer jobSynchronizer,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/spotify-top-tracks-albums.txt")
            Resource albumsPromptResource,
            @Value("classpath:prompts/spotify-top-tracks.txt")
            Resource tracksPromptResource,
            @Value("classpath:prompts/spotify-top-tracks-verification.txt")
            Resource verificationPromptResource,
            @Value("classpath:prompts/spotify-top-tracks-fallback.txt")
            Resource fallbackPromptResource,
            @Value("classpath:prompts/spotify-top-tracks-fallback-verification.txt")
            Resource fallbackVerificationPromptResource
    ) throws IOException {
        this.llmOperation = llmOperation;
        this.artistRepository = artistRepository;
        this.resultReader = resultReader;
        this.libraryService = libraryService;
        this.logService = logService;
        this.jobSynchronizer = jobSynchronizer;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        albumsPrompt = albumsPromptResource.getContentAsString(UTF_8);
        tracksPrompt = tracksPromptResource.getContentAsString(UTF_8);
        verificationPrompt = verificationPromptResource.getContentAsString(UTF_8);
        fallbackPrompt = fallbackPromptResource.getContentAsString(UTF_8);
        fallbackVerificationPrompt = fallbackVerificationPromptResource.getContentAsString(UTF_8);
    }

    public @Nullable List<String> discover(DiscoveryTask task, boolean cacheEnabled) {
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignored = jobSynchronizer.registerDiscoveryTask()) {
            jobSynchronizer.interruptDiscoveryIfStopped();
            String artistId = requireNonNull(task.getArtist(),
                    () -> "Discovery task '" + task.getId() + "' has no associated artist.").getId();
            SpotifyArtistData data = readArtistData(artistId);
            if (data == null || data.topTracks() == null || data.topTracks().isEmpty()) {
                return List.of();
            }
            Map<String, String> albumMatches = matchAlbums(task, artistId, data.topTracks(), cacheEnabled);
            List<String> matches = matchTracks(task, artistId, data.topTracks(), albumMatches, cacheEnabled);
            if (matches.contains(null)) {
                matchFallbackTracks(task, artistId, data.topTracks(), matches, cacheEnabled);
            }
            return matches.stream().allMatch(Objects::isNull) ? null : matches;
        }
    }

    private @Nullable SpotifyArtistData readArtistData(String artistId) {
        return transactionTemplate.execute(status -> resultReader.readSpotifyArtistData(artistId));
    }

    private Map<String, String> matchAlbums(
            DiscoveryTask task, String artistId, List<SpotifyArtistData.TopTrack> topTracks, boolean cacheEnabled
    ) {
        List<AlbumData> albums = readAlbums(artistId);
        Map<String, String> spotifyAlbums = new LinkedHashMap<>();
        topTracks.forEach(track -> spotifyAlbums.putIfAbsent(track.albumId(), track.albumTitle()));
        Request request = new Request(albumsPrompt,
                "Spotify albums (ID to title): " + JsonConverter.toJson(spotifyAlbums)
                        + "\nAlbums from the database: " + JsonConverter.toJson(albums));
        return llmOperation.call(task, request, AlbumMatchesResponse.class,
                cacheEnabled ? new DiscoveryTaskLlmExecutor.CacheSettings(SPOTIFY, "SPOTIFY_TOP_TRACKS_ALBUMS", CACHE_VERSION) : null,
                response -> {
                    try {
                        return parseAlbumMatches(response, spotifyAlbums, albums);
                    } catch (JacksonException | IllegalStateException e) {
                        throw invalidResponse(artistId, e, verificationPrompt);
                    }
                });
    }

    private List<AlbumData> readAlbums(String artistId) {
        return requireNonNull(transactionTemplate.execute(status -> {
            Artist artist = artistRepository.findById(artistId).orElseThrow();
            return artist.getAlbums().stream().sorted()
                    .map(album -> new AlbumData(album.getId(), album.getName(), album.getYear()))
                    .toList();
        }));
    }

    private List<String> matchTracks(
            DiscoveryTask task, String artistId, List<SpotifyArtistData.TopTrack> topTracks,
            Map<String, String> albumMatches, boolean cacheEnabled
    ) {
        List<AlbumTracks> albumTracks = readMatchedAlbumTracks(artistId, albumMatches);
        Request request = new Request(tracksPrompt,
                "Spotify top tracks in original order: " + JsonConverter.toJson(topTracks)
                        + "\nAlbum matches (Spotify ID to database ID): " + JsonConverter.toJson(albumMatches)
                        + "\nDatabase tracks grouped by album: " + JsonConverter.toJson(albumTracks));
        List<String> matchedAlbumIds = topTracks.stream().map(track -> albumMatches.get(track.albumId())).toList();
        return llmOperation.call(task, request, TrackMatchesResponse.class,
                cacheEnabled ? new DiscoveryTaskLlmExecutor.CacheSettings(SPOTIFY, "SPOTIFY_TOP_TRACKS", CACHE_VERSION) : null,
                response -> {
                    try {
                        return parseTrackMatches(response, matchedAlbumIds, albumTracks);
                    } catch (JacksonException | IllegalStateException e) {
                        throw invalidResponse(artistId, e, verificationPrompt);
                    }
                });
    }

    private List<AlbumTracks> readMatchedAlbumTracks(String artistId, Map<String, String> albumMatches) {
        return requireNonNull(transactionTemplate.execute(status -> {
            Artist artist = artistRepository.findById(artistId).orElseThrow();
            return artist.getAlbums().stream().filter(album -> albumMatches.containsValue(album.getId())).sorted()
                    .map(album -> new AlbumTracks(album.getId(), album.getName(), album.getSongs().stream().sorted()
                            .map(song -> new TrackData(song.getId(), song.getName(), song.getDiscNumber(), song.getTrackNumber()))
                            .toList()))
                    .toList();
        }));
    }

    private void matchFallbackTracks(
            DiscoveryTask task, String artistId, List<SpotifyArtistData.TopTrack> topTracks,
            List<String> matches, boolean cacheEnabled
    ) {
        List<AlbumTracks> albums = requireNonNull(transactionTemplate.execute(status -> readFallbackAlbums(artistId)));
        for (AlbumTracks album : albums) {
            List<Integer> positions = new ArrayList<>();
            List<SpotifyArtistData.TopTrack> unmatchedTracks = new ArrayList<>();
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i) == null) {
                    positions.add(i);
                    unmatchedTracks.add(topTracks.get(i));
                }
            }
            Request request = new Request(fallbackPrompt,
                    "Unmatched Spotify top tracks in original order: " + JsonConverter.toJson(unmatchedTracks)
                            + "\nDatabase album with tracks: " + JsonConverter.toJson(album));
            List<String> fallbackMatches = llmOperation.call(task, request, TrackMatchesResponse.class,
                    cacheEnabled ? new DiscoveryTaskLlmExecutor.CacheSettings(SPOTIFY, "SPOTIFY_TOP_TRACKS_FALLBACK", CACHE_VERSION) : null,
                    response -> {
                        try {
                            return parseTrackMatches(response, Collections.nCopies(unmatchedTracks.size(), album.id()), List.of(album));
                        } catch (JacksonException | IllegalStateException e) {
                            throw invalidResponse(artistId, e, fallbackVerificationPrompt);
                        }
                    });
            for (int i = 0; i < positions.size(); i++) {
                matches.set(positions.get(i), fallbackMatches.get(i));
            }
            if (!matches.contains(null)) {
                break;
            }
        }
    }

    private List<AlbumTracks> readFallbackAlbums(String artistId) {
        Artist artist = artistRepository.findById(artistId).orElseThrow();
        List<FallbackAlbum> albums = new ArrayList<>();
        for (Album album : artist.getAlbums()) {
            AlbumDiscoveryData data = libraryService.getAlbumDiscoveryData(album.getId()).orElse(null);
            if (data == null || (
                    data.releaseType() != ReleaseType.LP &&
                            data.releaseType() != ReleaseType.EP &&
                            data.releaseType() != ReleaseType.COMPILATION
            )) {
                continue;
            }
            LocalDateTime releaseDate = data.releaseDate();
            if (releaseDate == null && album.getYear() != null) {
                releaseDate = LocalDateTime.of(album.getYear(), 1, 1, 0, 0);
            }
            AlbumTracks tracks = new AlbumTracks(album.getId(), album.getName(), album.getSongs().stream().sorted()
                    .map(song -> new TrackData(song.getId(), song.getName(), song.getDiscNumber(), song.getTrackNumber()))
                    .toList());
            albums.add(new FallbackAlbum(tracks, data.releaseType(), releaseDate));
        }
        return albums.stream()
                .sorted(
                        Comparator.comparingInt((FallbackAlbum album) ->
                                        switch (album.releaseType()) {
                                            case LP -> 0;
                                            case EP -> 1;
                                            default -> 2;
                                        })
                                .thenComparing(FallbackAlbum::releaseDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(album -> album.album().title(), Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(album -> album.album().id())
                )
                .map(FallbackAlbum::album)
                .toList();
    }

    private Map<String, String> parseAlbumMatches(@Nullable String response, Map<String, String> spotifyAlbums, List<AlbumData> albums) {
        AlbumMatchesResponse parsed = RESPONSE_MAPPER.convertValue(
                response != null ? LlmJsonConverter.fromJson(response) : null, AlbumMatchesResponse.class);
        checkState(parsed != null,
                "The LLM album response must be a JSON object containing only the albumMatches field.");
        checkState(parsed.albumMatches() != null, "The LLM albumMatches field must be a JSON array.");
        Map<String, String> result = new LinkedHashMap<>();
        for (AlbumMatch match : parsed.albumMatches()) {
            checkState(match != null,
                    "Each LLM album match must contain only spotifyAlbumId and databaseAlbumId.");
            String spotifyAlbumId = match.spotifyAlbumId();
            checkState(spotifyAlbumId != null && spotifyAlbums.containsKey(spotifyAlbumId),
                    "The LLM returned an unknown Spotify album ID: %s.", spotifyAlbumId);
            checkState(!result.containsKey(spotifyAlbumId), "The LLM returned a duplicate Spotify album ID: %s.", spotifyAlbumId);
            String albumId = match.databaseAlbumId();
            checkState(albumId == null || albums.stream().anyMatch(album -> album.id().equals(albumId)),
                    "The LLM returned an unknown database album ID: %s.", albumId);
            result.put(spotifyAlbumId, albumId);
        }
        checkState(result.keySet().equals(spotifyAlbums.keySet()),
                "The LLM album response must include every supplied Spotify album ID. Expected IDs: %s. Returned IDs: %s.",
                spotifyAlbums.keySet(), result.keySet());
        return result;
    }

    private List<String> parseTrackMatches(@Nullable String response, List<String> albumIds, List<AlbumTracks> albums) {
        TrackMatchesResponse parsed = RESPONSE_MAPPER.convertValue(
                response != null ? LlmJsonConverter.fromJson(response) : null, TrackMatchesResponse.class);
        checkState(parsed != null,
                "The LLM track response must be a JSON object containing only the trackMatches field.");
        checkState(parsed.trackMatches() != null, "The LLM trackMatches field must be a JSON array.");
        List<TrackMatch> matches = parsed.trackMatches();
        checkState(matches.size() == albumIds.size(), "The LLM returned %s track positions, expected %s.", matches.size(), albumIds.size());
        List<String> result = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            TrackMatch match = matches.get(i);
            checkState(match != null,
                    "Each LLM track match must contain only databaseTrackId.");
            String trackId = match.databaseTrackId();
            String albumId = albumIds.get(i);
            checkState(trackId == null || albums.stream()
                            .filter(album -> album.id().equals(albumId))
                            .flatMap(album -> album.tracks().stream()).anyMatch(track -> track.id().equals(trackId)),
                    "The LLM returned track ID '%s' outside the matched album '%s' at position %s.", trackId, albumId, i);
            result.add(trackId);
        }
        return result;
    }

    private LlmFollowUpException invalidResponse(String artistId, RuntimeException error, String prompt) {
        transactionTemplate.executeWithoutResult(status ->
                logService.warn(logger, "Invalid Spotify top tracks response from the LLM for artist '{}'.",
                        artistRepository.findById(artistId).orElseThrow().getName(), error));
        return new LlmFollowUpException(prompt.formatted(JsonConverter.toJson(error.getMessage())), error);
    }

    private record AlbumData(String id, @Nullable String title, @Nullable Integer year) {}

    private record TrackData(String id, @Nullable String title, @Nullable Integer discNumber, @Nullable Integer trackNumber) {}

    private record AlbumTracks(String id, @Nullable String title, List<TrackData> tracks) {}

    private record FallbackAlbum(AlbumTracks album, ReleaseType releaseType, @Nullable LocalDateTime releaseDate) {}

    private record AlbumMatchesResponse(List<AlbumMatch> albumMatches) {}

    private record AlbumMatch(
            String spotifyAlbumId,
            @JsonProperty(required = true) @Nullable String databaseAlbumId
    ) {}

    private record TrackMatchesResponse(List<TrackMatch> trackMatches) {}

    private record TrackMatch(@JsonProperty(required = true) @Nullable String databaseTrackId) {}

    private record Request(String systemPrompt, String userPrompt) implements DiscoveryTaskLlmExecutor.Request {
        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        }
    }
}
