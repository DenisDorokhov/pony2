package net.dorokhov.pony2.core.library.service.discovery.musicbrainz;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmJsonConverter;
import net.dorokhov.pony2.common.TextTemplate;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryTaskLlmExecutor;
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
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion.MUSICBRAINZ;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class MusicBrainzLlmService {

    static final String VARIOUS_ARTISTS = "Various Artists";

    private static final int CACHE_VERSION = 1;
    private static final int NORMALIZATION_CACHE_VERSION = 1;
    private static final String NORMALIZATION_CACHE_PREFIX = "MUSICBRAINZ_ALBUM_TITLE_NORMALIZATION";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConfigService configService;
    private final DiscoveryTaskLlmExecutor llmExecutor;
    private final LogService logService;
    private final TransactionTemplate transactionTemplate;
    private final TextTemplate releaseGroupSystemPrompt;
    private final TextTemplate releaseGroupUserPrompt;
    private final TextTemplate artistSystemPrompt;
    private final TextTemplate artistUserPrompt;
    private final TextTemplate normalizationSystemPrompt;
    private final TextTemplate normalizationUserPrompt;
    private final TextTemplate normalizationVerificationUserPrompt;
    private final TextTemplate artistSearchSystemPrompt;
    private final TextTemplate artistSearchUserPrompt;
    private final TextTemplate artistSearchVerificationUserPrompt;
    private final TextTemplate releaseGroupVerificationUserPrompt;
    private final TextTemplate artistVerificationUserPrompt;

    public MusicBrainzLlmService(
            ConfigService configService,
            DiscoveryTaskLlmExecutor llmExecutor,
            LogService logService,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/musicbrainz-album-data-release-group-system.vm")
            Resource releaseGroupSystemPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-release-group-user.vm")
            Resource releaseGroupUserPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-artist-system.vm")
            Resource artistSystemPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-artist-user.vm")
            Resource artistUserPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-normalization-system.vm")
            Resource normalizationSystemPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-normalization-user.vm")
            Resource normalizationUserPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-normalization-verify-user.vm")
            Resource normalizationVerificationUserPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-artist-search-system.vm")
            Resource artistSearchSystemPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-artist-search-user.vm")
            Resource artistSearchUserPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-artist-search-verify-user.vm")
            Resource artistSearchVerificationUserPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-release-group-verify-user.vm")
            Resource releaseGroupVerificationUserPrompt,
            @Value("classpath:prompts/musicbrainz-album-data-artist-verify-user.vm")
            Resource artistVerificationUserPrompt
    ) throws IOException {
        this.configService = configService;
        this.llmExecutor = llmExecutor;
        this.logService = logService;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        this.releaseGroupSystemPrompt = new TextTemplate(releaseGroupSystemPrompt);
        this.releaseGroupUserPrompt = new TextTemplate(releaseGroupUserPrompt);
        this.artistSystemPrompt = new TextTemplate(artistSystemPrompt);
        this.artistUserPrompt = new TextTemplate(artistUserPrompt);
        this.normalizationSystemPrompt = new TextTemplate(normalizationSystemPrompt);
        this.normalizationUserPrompt = new TextTemplate(normalizationUserPrompt);
        this.normalizationVerificationUserPrompt = new TextTemplate(normalizationVerificationUserPrompt);
        this.artistSearchSystemPrompt = new TextTemplate(artistSearchSystemPrompt);
        this.artistSearchUserPrompt = new TextTemplate(artistSearchUserPrompt);
        this.artistSearchVerificationUserPrompt = new TextTemplate(artistSearchVerificationUserPrompt);
        this.releaseGroupVerificationUserPrompt = new TextTemplate(releaseGroupVerificationUserPrompt);
        this.artistVerificationUserPrompt = new TextTemplate(artistVerificationUserPrompt);
    }

    NormalizationResponse normalizeTitle(DiscoveryTask task, AlbumInput input, boolean cacheEnabled) {
        String title = input.title();
        PreparationRequest request = new PreparationRequest(new PreparationRawRequest(
                normalizationSystemPrompt.render(),
                normalizationUserPrompt.render(Map.of("albumJson", JsonConverter.toJson(Map.of("albumTitle", title)))),
                configService.get().llmModel(),
                "none"
        ));
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, NORMALIZATION_CACHE_PREFIX, NORMALIZATION_CACHE_VERSION) : null;
        return llmExecutor.call(task, request, NormalizationResponse.class, cacheSettings, response -> {
            try {
                NormalizationResponse data = response != null ? LlmJsonConverter.fromJson(response, NormalizationResponse.class) : null;
                checkState(data != null, "The normalization response must be a JSON object.");
                checkState(data.searchTitle() != null && !data.searchTitle().isBlank(),
                        "searchTitle must be a non-blank string.");
                checkState(data.searchFragments() != null, "searchFragments must be an array.");
                checkState(data.searchFragments().size() <= 3, "searchFragments must contain at most three strings.");
                for (String fragment : data.searchFragments()) {
                    checkState(fragment != null && !fragment.isBlank(),
                            "Each searchFragments item must be a non-blank string.");
                    checkState(data.searchTitle().contains(fragment),
                            "Each searchFragments item must be a literal contiguous substring of searchTitle: %s", fragment);
                }
                checkState(data.alternativeTitles() != null, "alternativeTitles must be an array.");
                checkState(data.alternativeTitles().size() <= 3, "alternativeTitles must contain at most three strings.");
                for (String alternativeTitle : data.alternativeTitles()) {
                    checkState(alternativeTitle != null && !alternativeTitle.isBlank(),
                            "Each alternativeTitles item must be a non-blank string.");
                    checkState(title.contains(alternativeTitle),
                            "Each alternativeTitles item must be a literal contiguous substring of albumTitle: %s", alternativeTitle);
                }
                return data;
            } catch (JacksonException | IllegalStateException e) {
                throw invalidResponse(input, LlmStage.ALBUM_TITLE_NORMALIZATION, e);
            }
        });
    }

    private LlmFollowUpException invalidResponse(AlbumInput input, LlmStage stage, RuntimeException error) {
        String stageDescription = switch (stage) {
            case ALBUM_TITLE_NORMALIZATION -> "album title normalization";
            case ARTIST_SEARCH -> "alternative artist search";
            case RELEASE_GROUP_SELECTION -> "release group selection";
            case ARTIST_SELECTION -> "artist selection";
        };
        transactionTemplate.executeWithoutResult(status ->
                logService.warn(logger, "Invalid MusicBrainz response from the LLM for album '{}' -> '{}' during {}.",
                        input.artistName(), input.title(), stageDescription, error));
        TextTemplate prompt = switch (stage) {
            case ALBUM_TITLE_NORMALIZATION -> normalizationVerificationUserPrompt;
            case ARTIST_SEARCH -> artistSearchVerificationUserPrompt;
            case RELEASE_GROUP_SELECTION -> releaseGroupVerificationUserPrompt;
            case ARTIST_SELECTION -> artistVerificationUserPrompt;
        };
        return new LlmFollowUpException(prompt.render(Map.of("validationError", JsonConverter.toJson(error.getMessage()))), error);
    }

    List<String> selectSearchArtists(DiscoveryTask task, AlbumInput input, List<TrackInput> tracks, boolean cacheEnabled) {
        List<String> alternatives = new ArrayList<>();
        for (TrackInput track : tracks) {
            String name = track.artistName() != null ? track.artistName().strip() : "";
            if (!name.isBlank() && !name.equalsIgnoreCase(input.artistName().strip()) &&
                    alternatives.stream().noneMatch(existing -> existing.equalsIgnoreCase(name))) {
                alternatives.add(name);
            }
        }
        if (alternatives.isEmpty()) {
            return List.of();
        }
        PreparationRequest request = new PreparationRequest(new PreparationRawRequest(
                artistSearchSystemPrompt.render(),
                artistSearchUserPrompt.render(Map.of("searchContextJson",
                        JsonConverter.toJson(Map.of("album", input, "tracks", tracks, "alternativeArtistNames", alternatives)))),
                configService.get().llmModel(),
                "none"
        ));
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, "MUSICBRAINZ_ALBUM_ARTIST_SEARCH", CACHE_VERSION) : null;
        return llmExecutor.call(task, request, ArtistNamesResponse.class, cacheSettings, response -> {
            try {
                ArtistNamesResponse data = response != null ? LlmJsonConverter.fromJson(response, ArtistNamesResponse.class) : null;
                checkState(data != null, "The artist search response must be a JSON object.");
                checkState(data.artistNames() != null, "artistNames must be an array.");
                List<String> names = new ArrayList<>();
                for (String value : data.artistNames()) {
                    checkState(value != null && !value.isBlank(), "Each artistNames item must be a non-blank string.");
                    String name = value.strip();
                    checkState(!name.equalsIgnoreCase(input.artistName().strip()), "Do not repeat the original library artist.");
                    String allowed = name.equalsIgnoreCase(VARIOUS_ARTISTS) ? VARIOUS_ARTISTS
                            : alternatives.stream().filter(candidate -> candidate.equalsIgnoreCase(name)).findFirst().orElse(null);
                    checkState(allowed != null, "Choose an artist name from alternativeArtistNames or Various Artists: %s", name);
                    if (names.stream().noneMatch(existing -> existing.equalsIgnoreCase(allowed))) {
                        names.add(allowed);
                    }
                }
                return List.copyOf(names);
            } catch (JacksonException | IllegalStateException e) {
                throw invalidResponse(input, LlmStage.ARTIST_SEARCH, e);
            }
        });
    }

    @Nullable String selectReleaseGroup(
            DiscoveryTask task, AlbumInput input, MusicBrainzClient.ReleaseGroupSearchResult searchResult,
            List<TrackInput> tracks, List<String> searchArtistNames, boolean cacheEnabled
    ) {
        String prompt = releaseGroupSystemPrompt.render(Map.of("hasAlternativeArtists", !searchArtistNames.isEmpty()));
        Map<String, Object> variables = selectionVariables(input, searchArtistNames);
        addTrackContext(variables, tracks);
        Request request = selectionRequest(releaseGroupUserPrompt, prompt, searchResult.rawResponse(), variables);
        Set<String> candidates = searchResult.groups().stream().map(MusicBrainzDto.ReleaseGroup::id).collect(Collectors.toSet());
        return selectId(task, input, request, candidates, "MUSICBRAINZ_ALBUM_DATA_RELEASE_GROUP",
                LlmStage.RELEASE_GROUP_SELECTION, cacheEnabled);
    }

    private Map<String, Object> selectionVariables(AlbumInput input, List<String> searchArtistNames) {
        return new LinkedHashMap<>(Map.of(
                "albumJson", JsonConverter.toJson(input),
                "hasAlternativeArtists", !searchArtistNames.isEmpty(),
                "alternativeArtistNamesJson", JsonConverter.toJson(searchArtistNames)));
    }

    private void addTrackContext(Map<String, Object> variables, List<TrackInput> tracks) {
        variables.put("trackCount", tracks.size());
        variables.put("tracksJson", JsonConverter.toJson(tracks));
    }

    private Request selectionRequest(TextTemplate userPrompt, String systemPrompt, String response, Map<String, Object> variables) {
        variables.put("responseJson", responseJson(response, false));
        RawRequest rawRequest = new RawRequest(systemPrompt, userPrompt.render(variables), configService.get().llmModel());
        variables.put("responseJson", responseJson(response, true));
        return new Request(rawRequest, new CacheKey(systemPrompt, userPrompt.render(variables)));
    }

    private String responseJson(String response, boolean forCache) {
        Map<String, Object> data = JsonConverter.mapFromJson(response, String.class, Object.class);
        if (forCache) {
            data.remove("created");
        }
        return JsonConverter.toJson(data);
    }

    private @Nullable String selectId(
            DiscoveryTask task, AlbumInput input, Request request, Set<String> candidates,
            String keyPrefix, LlmStage stage, boolean cacheEnabled
    ) {
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, keyPrefix, CACHE_VERSION) : null;
        return llmExecutor.call(task, request, IdResponse.class, cacheSettings, response -> {
            try {
                IdResponse data = response != null ? LlmJsonConverter.fromJson(response, IdResponse.class) : null;
                checkState(data != null, "The LLM response must be a JSON object containing the id field.");
                String id = data.id();
                checkState(id == null || candidates.contains(id),
                        "The LLM must return one of the supplied candidate IDs or null in the id field.");
                return id;
            } catch (JacksonException | IllegalStateException e) {
                throw invalidResponse(input, stage, e);
            }
        });
    }

    @Nullable String selectArtist(
            DiscoveryTask task, AlbumInput input, ReleaseGroupSelection selection, List<TrackInput> tracks, boolean cacheEnabled
    ) {
        MusicBrainzDto.ReleaseGroup group = selection.group();
        List<String> searchArtistNames = selection.searchArtistNames();
        List<String> ids = group.artistCredit().stream().map(credit -> credit.artist().id()).distinct().toList();
        String prompt = artistSystemPrompt.render(Map.of("hasAlternativeArtists", !searchArtistNames.isEmpty()));
        Map<String, Object> variables = selectionVariables(input, searchArtistNames);
        variables.put("selectedGroupIdJson", JsonConverter.toJson(group.id()));
        if (!searchArtistNames.isEmpty()) {
            addTrackContext(variables, tracks);
        }
        Request request = selectionRequest(artistUserPrompt, prompt, selection.response(), variables);
        return selectId(task, input, request, Set.copyOf(ids), "MUSICBRAINZ_ALBUM_DATA_ARTIST",
                LlmStage.ARTIST_SELECTION, cacheEnabled);
    }

    private enum LlmStage {
        ALBUM_TITLE_NORMALIZATION,
        ARTIST_SEARCH,
        RELEASE_GROUP_SELECTION,
        ARTIST_SELECTION
    }

    record AlbumInput(String title, @Nullable Integer year, String artistName) {
    }

    record TrackInput(
            @Nullable String title, @Nullable Integer discNumber, @Nullable Integer trackNumber, Long durationSeconds,
            @Nullable String artistName, @Nullable String albumArtistName
    ) {
    }

    record ReleaseGroupSelection(MusicBrainzDto.ReleaseGroup group, String response, List<String> searchArtistNames) {
    }

    private record IdResponse(@Nullable String id) {
    }

    private record ArtistNamesResponse(List<String> artistNames) {
    }

    record NormalizationResponse(String searchTitle, List<String> searchFragments, List<String> alternativeTitles) {
    }

    private record PreparationRawRequest(
            String systemPrompt, String userPrompt, String model, String toolChoice
    ) {}

    private record PreparationCacheKey(String systemPrompt, String userPrompt, String toolChoice) {}

    private record PreparationRequest(PreparationRawRequest rawRequest) implements DiscoveryTaskLlmExecutor.Request {

        @Override
        public PreparationCacheKey cacheKey() {
            return new PreparationCacheKey(rawRequest.systemPrompt(), rawRequest.userPrompt(), rawRequest.toolChoice());
        }

        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(rawRequest.systemPrompt()), new UserMessage(rawRequest.userPrompt())),
                    OpenAiChatOptions.builder().model(rawRequest.model()).toolChoice(rawRequest.toolChoice()).build());
        }
    }

    private record RawRequest(String systemPrompt, String userPrompt, String model) {}

    private record CacheKey(String systemPrompt, String userPrompt) {}

    private record Request(RawRequest rawRequest, CacheKey cacheKey) implements DiscoveryTaskLlmExecutor.Request {
        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(rawRequest.systemPrompt()), new UserMessage(rawRequest.userPrompt())),
                    OpenAiChatOptions.builder().model(rawRequest.model()).toolChoice("none").build());
        }
    }
}
