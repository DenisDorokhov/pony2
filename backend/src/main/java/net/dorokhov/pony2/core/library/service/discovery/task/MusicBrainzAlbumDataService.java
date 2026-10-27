package net.dorokhov.pony2.core.library.service.discovery.task;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.MusicBrainzAlbumData;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmJsonConverter;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.installation.service.BuildVersionProvider;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryTaskLlmExecutor;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzClient;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzDto;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.library.service.exception.LlmFollowUpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.llm.domain.DiscoveryCacheRegion.MUSICBRAINZ;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class MusicBrainzAlbumDataService {

    private static final int CACHE_VERSION = 1;

    private static final int NORMALIZATION_CACHE_VERSION = 1;
    private static final String NORMALIZATION_CACHE_PREFIX = "MUSICBRAINZ_ALBUM_TITLE_NORMALIZATION";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MusicBrainzClient client;
    private final AlbumRepository albumRepository;
    private final DiscoveryTaskRepository taskRepository;
    private final UserService userService;
    private final BuildVersionProvider buildVersionProvider;
    private final ConfigService configService;
    private final DiscoveryTaskLlmExecutor llmExecutor;
    private final LogService logService;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final ShutdownService shutdownService;
    private final TransactionTemplate transactionTemplate;

    private final String releaseGroupPrompt;
    private final String artistPrompt;
    private final String verificationPrompt;
    private final String normalizationPrompt;
    private final String normalizationVerificationPrompt;

    public MusicBrainzAlbumDataService(
            MusicBrainzClient client,
            AlbumRepository albumRepository,
            DiscoveryTaskRepository taskRepository,
            UserService userService,
            BuildVersionProvider buildVersionProvider,
            ConfigService configService,
            DiscoveryTaskLlmExecutor llmExecutor,
            LogService logService,
            LibraryJobSynchronizer jobSynchronizer,
            ShutdownService shutdownService,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/musicbrainz-release-group.txt") Resource releaseGroupPrompt,
            @Value("classpath:prompts/musicbrainz-artist.txt") Resource artistPrompt,
            @Value("classpath:prompts/musicbrainz-verification.txt") Resource verificationPrompt,
            @Value("classpath:prompts/musicbrainz-album-normalization.txt") Resource normalizationPrompt,
            @Value("classpath:prompts/musicbrainz-album-normalization-verification.txt") Resource normalizationVerificationPrompt
    ) throws IOException {
        this.client = client;
        this.albumRepository = albumRepository;
        this.taskRepository = taskRepository;
        this.userService = userService;
        this.buildVersionProvider = buildVersionProvider;
        this.configService = configService;
        this.llmExecutor = llmExecutor;
        this.logService = logService;
        this.jobSynchronizer = jobSynchronizer;
        this.shutdownService = shutdownService;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        this.releaseGroupPrompt = releaseGroupPrompt.getContentAsString(UTF_8);
        this.artistPrompt = artistPrompt.getContentAsString(UTF_8);
        this.verificationPrompt = verificationPrompt.getContentAsString(UTF_8);
        this.normalizationPrompt = normalizationPrompt.getContentAsString(UTF_8);
        this.normalizationVerificationPrompt = normalizationVerificationPrompt.getContentAsString(UTF_8);
    }

    public @Nullable MusicBrainzAlbumData discover(DiscoveryTask task, boolean cacheEnabled) {
        interruptIfNeeded();
        String albumId = requireNonNull(task.getAlbum(),
                () -> "Discovery task '" + task.getId() + "' has no associated album.").getId();
        AlbumInput input = requireNonNull(transactionTemplate.execute(status -> {
            Album album = albumRepository.findById(albumId).orElseThrow();
            return new AlbumInput(album.getName(), album.getYear(), album.getArtist().getName());
        }));
        if (input.title() == null || input.title().isBlank() || input.artistName() == null || input.artistName().isBlank()) {
            logger.debug("Skipping MusicBrainz discovery for album '{}': album title or artist name is unavailable.", albumId);
            return null;
        }
        String userAgent = "pony2/" + buildVersionProvider.getBuildVersion().getVersion() + " (" + userService.getMainAdmin().getEmail() + ")";
        MusicBrainzClient.ExchangeObserver observer = new MusicBrainzClient.ExchangeObserver() {
            @Override
            public void onRequest(String url) {
                interruptIfNeeded();
                saveExchange(task, url, null);
            }

            @Override
            public void onResponse(String url, String response) {
                saveExchange(task, url, response);
            }
        };
        try (
                MusicBrainzClient.Session session = client.openSession(userAgent, observer, cacheEnabled);
                LibraryJobSynchronizer.CancellationSubscription ignored = jobSynchronizer.onCancel(session::cancel)
        ) {
            String query = "release:\"" + escapeQuery(input.title()) + "\" AND artistname:\"" + escapeQuery(input.artistName()) + "\"";
            MusicBrainzClient.ReleaseGroupSearchResult searchResult = session.searchReleaseGroups(query);
            interruptIfNeeded();
            if (searchResult.groups().isEmpty()) {
                NormalizationResponse normalization = normalizeTitle(task, input.title(), cacheEnabled);
                String normalizedQuery = normalizationQuery(input, normalization);
                interruptIfNeeded();
                if (normalizedQuery != null) {
                    searchResult = session.searchReleaseGroups(normalizedQuery);
                    interruptIfNeeded();
                }
            }
            MusicBrainzDto.ReleaseGroup group = selectReleaseGroup(task, input, searchResult.groups(), searchResult.rawResponse(), cacheEnabled);
            if (group == null) {
                logger.debug("No unambiguous MusicBrainz release group was selected for album '{}'.", albumId);
                return null;
            }
            String artistId = selectArtist(task, input, group, searchResult.rawResponse(), cacheEnabled);
            if (artistId == null) {
                logger.debug("No corresponding MusicBrainz artist was selected for album '{}' in release group '{}'.", albumId, group.id());
                return null;
            }
            MusicBrainzDto.Artist artist = session.getArtist(artistId);
            interruptIfNeeded();
            List<String> country = artist.area() != null ? artist.area().country() : List.of();
            if (country.isEmpty() && artist.country() != null && !artist.country().isBlank()) {
                country = List.of(artist.country());
            }
            MusicBrainzAlbumData result = new MusicBrainzAlbumData(group.id(), group.primaryType(), group.secondaryTypes(), group.firstReleaseDate(),
                    artist.id(), country);
            logger.debug("MusicBrainz discovery result for album '{}': {}.", albumId, result);
            return result;
        } catch (MusicBrainzClient.CancelledException e) {
            throw new DiscoveryInterruptedException();
        }
    }

    private NormalizationResponse normalizeTitle(DiscoveryTask task, String title, boolean cacheEnabled) {
        NormalizationRequest request = new NormalizationRequest(normalizationPrompt,
                JsonConverter.toJson(Map.of("albumTitle", title)), configService.get().llmModel(), 0.0, "none");
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, NORMALIZATION_CACHE_PREFIX, NORMALIZATION_CACHE_VERSION) : null;
        return llmExecutor.call(task, request, NormalizationResponse.class, cacheSettings, response -> {
            try {
                Object parsed = response != null ? LlmJsonConverter.fromJson(response) : null;
                checkState(parsed instanceof Map<?, ?>, "The normalization response must be a JSON object.");
                Map<?, ?> data = (Map<?, ?>) parsed;
                checkState(data.keySet().equals(Set.of("searchTitle", "searchFragments", "alternativeTitles")),
                        "The normalization response must contain exactly searchTitle, searchFragments and alternativeTitles.");
                Object searchTitle = data.get("searchTitle");
                checkState(searchTitle instanceof String && !((String) searchTitle).isBlank(),
                        "searchTitle must be a non-blank string.");
                Object searchFragments = data.get("searchFragments");
                checkState(searchFragments instanceof List<?>, "searchFragments must be an array.");
                List<?> fragments = (List<?>) searchFragments;
                checkState(fragments.size() <= 3, "searchFragments must contain at most three strings.");
                List<String> validatedFragments = new ArrayList<>();
                for (Object fragment : fragments) {
                    checkState(fragment instanceof String && !((String) fragment).isBlank(),
                            "Each searchFragments item must be a non-blank string.");
                    checkState(((String) searchTitle).contains((String) fragment),
                            "Each searchFragments item must be a literal contiguous substring of searchTitle: %s", fragment);
                    validatedFragments.add((String) fragment);
                }
                Object alternativeTitles = data.get("alternativeTitles");
                checkState(alternativeTitles instanceof List<?>, "alternativeTitles must be an array.");
                List<?> titles = (List<?>) alternativeTitles;
                checkState(titles.size() <= 3, "alternativeTitles must contain at most three strings.");
                List<String> validatedTitles = new ArrayList<>();
                for (Object alternativeTitle : titles) {
                    checkState(alternativeTitle instanceof String && !((String) alternativeTitle).isBlank(),
                            "Each alternativeTitles item must be a non-blank string.");
                    checkState(title.contains((String) alternativeTitle),
                            "Each alternativeTitles item must be a literal contiguous substring of albumTitle: %s", alternativeTitle);
                    validatedTitles.add((String) alternativeTitle);
                }
                return new NormalizationResponse((String) searchTitle, validatedFragments, validatedTitles);
            } catch (RuntimeException e) {
                throw invalidResponse(task, LlmStage.ALBUM_TITLE_NORMALIZATION, e);
            }
        });
    }

    private @Nullable String normalizationQuery(AlbumInput input, NormalizationResponse normalization) {
        String original = "release:\"" + escapeQuery(input.title()) + "\"";
        List<UnaryOperator<String>> normalizers = List.of(
                UnaryOperator.identity(),
                term -> term.replaceAll("[^\\p{L}\\p{N} ]", " ").replaceAll(" +", " ").strip(),
                term -> term.replaceAll("[^\\p{L}\\p{N} ]", "").replaceAll(" +", " ").strip());
        List<String> branches = new ArrayList<>();
        for (UnaryOperator<String> normalizer : normalizers) {
            String title = normalizer.apply(normalization.searchTitle());
            String full = "release:\"" + escapeQuery(title) + "\"";
            if (!title.isBlank() && !full.equals(original) && !branches.contains(full)) {
                branches.add(full);
            }
            for (String alternativeTitle : normalization.alternativeTitles()) {
                String normalizedTitle = normalizer.apply(alternativeTitle);
                String alternative = "release:\"" + escapeQuery(normalizedTitle) + "\"";
                if (!normalizedTitle.isBlank() && !alternative.equals(original) && !branches.contains(alternative)) {
                    branches.add(alternative);
                }
            }
            List<String> normalizedFragments = normalization.searchFragments().stream().map(normalizer).distinct().toList();
            if (!normalizedFragments.isEmpty() && normalizedFragments.stream().noneMatch(String::isBlank)) {
                List<String> fragmentClauses = normalizedFragments.stream()
                        .map(fragment -> "release:\"" + escapeQuery(fragment) + "\"").toList();
                String fragments = String.join(" AND ", fragmentClauses);
                String branch = fragmentClauses.size() > 1 ? "(" + fragments + ")" : fragments;
                if (!fragments.equals(original) && !branches.contains(branch)) {
                    branches.add(branch);
                }
            }
        }
        if (branches.isEmpty()) {
            return null;
        }
        String conditions = branches.size() > 1 ? "(" + String.join(" OR ", branches) + ")" : branches.getFirst();
        return "artistname:\"" + escapeQuery(input.artistName()) + "\" AND " + conditions;
    }

    private @Nullable MusicBrainzDto.ReleaseGroup selectReleaseGroup(
            DiscoveryTask task, AlbumInput input,
            List<MusicBrainzDto.ReleaseGroup> groups, String response,
            boolean cacheEnabled
    ) {
        Map<String, MusicBrainzDto.ReleaseGroup> candidates = new LinkedHashMap<>();
        groups.forEach(group -> candidates.putIfAbsent(group.id(), group));
        List<MusicBrainzDto.ReleaseGroup> exact = candidates.values().stream()
                .filter(group -> group.releases().stream().anyMatch(release -> input.title().equalsIgnoreCase(release.title())))
                .toList();
        if (exact.size() == 1) {
            return exact.getFirst();
        }
        if (candidates.isEmpty()) {
            return null;
        }
        String albumId = requireNonNull(task.getAlbum(),
                () -> "Discovery task '" + task.getId() + "' has no associated album.").getId();
        List<TrackInput> tracks = requireNonNull(transactionTemplate.execute(status ->
                albumRepository.findById(albumId).orElseThrow().getSongs().stream()
                        .map(song -> new TrackInput(song.getName(), song.getDiscNumber(), song.getTrackNumber(), song.getDuration(),
                                song.getArtistName(), song.getAlbumArtistName()))
                        .sorted(Comparator.comparingInt((TrackInput track) ->
                                        track.discNumber() != null && track.discNumber() > 0 ? track.discNumber() : 1)
                                .thenComparing(TrackInput::trackNumber, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(TrackInput::title, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList()));
        String trackContext = "\nLibrary album track count: " + tracks.size()
                + "\nLibrary album tracks: " + JsonConverter.toJson(tracks);
        Request request = new Request(releaseGroupPrompt, context(input, response, false) + trackContext,
                context(input, response, true) + trackContext,
                configService.get().llmModel());
        String id = selectId(task, request, candidates.keySet(), "MUSICBRAINZ_ALBUM_DATA_RELEASE_GROUP",
                LlmStage.RELEASE_GROUP_SELECTION, cacheEnabled);
        return id != null ? candidates.get(id) : null;
    }

    private @Nullable String selectArtist(
            DiscoveryTask task, AlbumInput input, MusicBrainzDto.ReleaseGroup group,
            String response, boolean cacheEnabled
    ) {
        List<String> ids = group.artistCredit().stream().map(credit -> credit.artist().id()).distinct().toList();
        if (ids.size() == 1) {
            return ids.getFirst();
        }
        List<String> exact = group.artistCredit().stream()
                .filter(credit -> input.artistName().equalsIgnoreCase(credit.artist().name())
                        || input.artistName().equalsIgnoreCase(credit.name()))
                .map(credit -> credit.artist().id()).distinct().toList();
        if (exact.size() == 1) {
            return exact.getFirst();
        }
        if (ids.isEmpty()) {
            return null;
        }
        String selectedGroup = "\nSelected release group ID: " + JsonConverter.toJson(group.id());
        Request request = new Request(artistPrompt, context(input, response, false) + selectedGroup,
                context(input, response, true) + selectedGroup, configService.get().llmModel());
        return selectId(task, request, Set.copyOf(ids), "MUSICBRAINZ_ALBUM_DATA_ARTIST",
                LlmStage.ARTIST_SELECTION, cacheEnabled);
    }

    private @Nullable String selectId(
            DiscoveryTask task, Request request, Set<String> candidates,
            String keyPrefix, LlmStage stage, boolean cacheEnabled
    ) {
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, keyPrefix, CACHE_VERSION) : null;
        return llmExecutor.call(task, request, IdResponse.class, cacheSettings, response -> {
            try {
                Object parsed = response != null ? LlmJsonConverter.fromJson(response) : null;
                checkState(parsed instanceof Map<?, ?>, "The LLM response must be a JSON object containing the id field.");
                Map<?, ?> data = (Map<?, ?>) parsed;
                checkState(data.keySet().equals(Set.of("id")), "The LLM response must contain only the id field.");
                Object id = data.get("id");
                checkState(id == null || (id instanceof String && candidates.contains(id)),
                        "The LLM must return one of the supplied candidate IDs or null in the id field.");
                return (String) id;
            } catch (RuntimeException e) {
                throw invalidResponse(task, stage, e);
            }
        });
    }

    private LlmFollowUpException invalidResponse(DiscoveryTask task, LlmStage stage, RuntimeException error) {
        String albumId = requireNonNull(task.getAlbum(),
                () -> "Discovery task '" + task.getId() + "' has no associated album.").getId();
        String stageDescription = switch (stage) {
            case ALBUM_TITLE_NORMALIZATION -> "album title normalization";
            case RELEASE_GROUP_SELECTION -> "release group selection";
            case ARTIST_SELECTION -> "artist selection";
        };
        transactionTemplate.executeWithoutResult(status -> {
            Album album = albumRepository.findById(albumId).orElseThrow();
            logService.warn(logger, "Invalid MusicBrainz response from the LLM for album '{}' -> '{}' during {}.",
                    album.getArtist().getName(), album.getName(), stageDescription, error);
        });
        String prompt = stage == LlmStage.ALBUM_TITLE_NORMALIZATION ? normalizationVerificationPrompt : verificationPrompt;
        return new LlmFollowUpException(prompt.formatted(JsonConverter.toJson(error.getMessage())), error);
    }

    private String context(AlbumInput input, String response, boolean forCache) {
        Map<String, Object> data = JsonConverter.mapFromJson(response, String.class, Object.class);
        if (forCache) {
            data.remove("created");
        }
        return "Album from the library: " + JsonConverter.toJson(input)
                + "\nMusicBrainz search response: " + JsonConverter.toJson(new SearchResponse(data));
    }

    private String escapeQuery(String value) {
        return value.replaceAll("([+\\-!(){}\\[\\]^\"~*?:\\\\/|&])", "\\\\$1");
    }

    private void saveExchange(DiscoveryTask task, String url, @Nullable String response) {
        List<Object> requests = task.getRawRequest() != null
                ? new ArrayList<>(JsonConverter.listFromJson(task.getRawRequest(), Object.class)) : new ArrayList<>();
        List<String> responses = task.getRawResult() != null
                ? new ArrayList<>(JsonConverter.listFromJson(task.getRawResult(), String.class)) : new ArrayList<>();
        if (response == null) {
            requests.add(url);
            responses.add(null);
        } else {
            checkState(!requests.isEmpty() && url.equals(requests.getLast()), "MusicBrainz exchanges must be sequential.");
            responses.set(responses.size() - 1, response);
        }
        transactionTemplate.executeWithoutResult(status -> taskRepository.save(task
                .setRawRequest(JsonConverter.toJson(requests)).setRawResult(JsonConverter.toJson(responses))));
    }

    private void interruptIfNeeded() {
        jobSynchronizer.interruptDiscoveryIfCancelled();
        if (shutdownService.isShutdown()) {
            throw new DiscoveryInterruptedException();
        }
    }

    private enum LlmStage {
        ALBUM_TITLE_NORMALIZATION,
        RELEASE_GROUP_SELECTION,
        ARTIST_SELECTION
    }

    @JsonPropertyOrder({"title", "year", "artistName"})
    private record AlbumInput(@Nullable String title, @Nullable Integer year, @Nullable String artistName) {
    }

    @JsonPropertyOrder({"title", "discNumber", "trackNumber", "durationSeconds", "artistName", "albumArtistName"})
    private record TrackInput(
            @Nullable String title, @Nullable Integer discNumber, @Nullable Integer trackNumber, Long durationSeconds,
            @Nullable String artistName, @Nullable String albumArtistName
    ) {
    }

    private record SearchResponse(
            @JsonAnyGetter @JsonFormat(with = WRITE_SORTED_MAP_ENTRIES) Map<String, Object> data
    ) {
    }

    private record IdResponse(@JsonProperty(required = true) @Nullable String id) {
    }

    @JsonPropertyOrder({"searchTitle", "searchFragments", "alternativeTitles"})
    private record NormalizationResponse(String searchTitle, List<String> searchFragments, List<String> alternativeTitles) {
    }

    @JsonPropertyOrder({"systemPrompt", "userPrompt", "model", "temperature", "toolChoice"})
    private record NormalizationRequest(
            String systemPrompt, String userPrompt, String model, double temperature, String toolChoice
    ) implements DiscoveryTaskLlmExecutor.Request {
        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                    OpenAiChatOptions.builder().model(model).temperature(temperature).toolChoice(toolChoice).build());
        }
    }

    @JsonPropertyOrder({"systemPrompt", "userPrompt"})
    private record Request(
            String systemPrompt, String userPrompt, @JsonIgnore String cacheUserPrompt, @JsonIgnore String model
    ) implements DiscoveryTaskLlmExecutor.Request {
        @Override
        public String toCacheJson() {
            return JsonConverter.toJson(new Request(systemPrompt, cacheUserPrompt, cacheUserPrompt, model));
        }

        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                    OpenAiChatOptions.builder().model(model).toolChoice("none").build());
        }
    }
}
