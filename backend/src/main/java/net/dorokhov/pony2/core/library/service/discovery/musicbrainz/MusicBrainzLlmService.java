package net.dorokhov.pony2.core.library.service.discovery.musicbrainz;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmJsonConverter;
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
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion.MUSICBRAINZ;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class MusicBrainzLlmService {

    static final String VARIOUS_ARTISTS = "Various Artists";

    private static final int CACHE_VERSION = 1;
    private static final int NORMALIZATION_CACHE_VERSION = 1;
    private static final String NORMALIZATION_CACHE_PREFIX = "MUSICBRAINZ_ALBUM_TITLE_NORMALIZATION";

    private static final JsonMapper RESPONSE_MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConfigService configService;
    private final DiscoveryTaskLlmExecutor llmExecutor;
    private final LogService logService;
    private final TransactionTemplate transactionTemplate;

    private final String releaseGroupPrompt;
    private final String artistPrompt;
    private final String verificationPrompt;
    private final String normalizationPrompt;
    private final String normalizationVerificationPrompt;
    private final String artistSearchPrompt;
    private final String artistSearchVerificationPrompt;
    private final String alternativeArtistInstructions;

    public MusicBrainzLlmService(
            ConfigService configService,
            DiscoveryTaskLlmExecutor llmExecutor,
            LogService logService,
            PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/musicbrainz-release-group.txt")
            Resource releaseGroupPrompt,
            @Value("classpath:prompts/musicbrainz-artist.txt")
            Resource artistPrompt,
            @Value("classpath:prompts/musicbrainz-verification.txt")
            Resource verificationPrompt,
            @Value("classpath:prompts/musicbrainz-album-normalization.txt")
            Resource normalizationPrompt,
            @Value("classpath:prompts/musicbrainz-album-normalization-verification.txt")
            Resource normalizationVerificationPrompt,
            @Value("classpath:prompts/musicbrainz-artist-search.txt")
            Resource artistSearchPrompt,
            @Value("classpath:prompts/musicbrainz-artist-search-verification.txt")
            Resource artistSearchVerificationPrompt,
            @Value("classpath:prompts/musicbrainz-alternative-artist-instructions.txt")
            Resource alternativeArtistInstructions
    ) throws IOException {
        this.configService = configService;
        this.llmExecutor = llmExecutor;
        this.logService = logService;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        this.releaseGroupPrompt = releaseGroupPrompt.getContentAsString(UTF_8);
        this.artistPrompt = artistPrompt.getContentAsString(UTF_8);
        this.verificationPrompt = verificationPrompt.getContentAsString(UTF_8);
        this.normalizationPrompt = normalizationPrompt.getContentAsString(UTF_8);
        this.normalizationVerificationPrompt = normalizationVerificationPrompt.getContentAsString(UTF_8);
        this.artistSearchPrompt = artistSearchPrompt.getContentAsString(UTF_8);
        this.artistSearchVerificationPrompt = artistSearchVerificationPrompt.getContentAsString(UTF_8);
        this.alternativeArtistInstructions = "\n" + alternativeArtistInstructions.getContentAsString(UTF_8).strip();
    }

    public NormalizationResponse normalizeTitle(DiscoveryTask task, AlbumInput input, boolean cacheEnabled) {
        String title = input.title();
        PreparationRequest request = new PreparationRequest(normalizationPrompt,
                JsonConverter.toJson(Map.of("albumTitle", title)), configService.get().llmModel(), 0.0, "none");
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, NORMALIZATION_CACHE_PREFIX, NORMALIZATION_CACHE_VERSION) : null;
        return llmExecutor.call(task, request, NormalizationResponse.class, cacheSettings, response -> {
            try {
                NormalizationResponse data = RESPONSE_MAPPER.convertValue(
                        response != null ? LlmJsonConverter.fromJson(response) : null, NormalizationResponse.class);
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
        String prompt = switch (stage) {
            case ALBUM_TITLE_NORMALIZATION -> normalizationVerificationPrompt;
            case ARTIST_SEARCH -> artistSearchVerificationPrompt;
            default -> verificationPrompt;
        };
        return new LlmFollowUpException(prompt.formatted(JsonConverter.toJson(error.getMessage())), error);
    }

    public List<String> selectSearchArtists(DiscoveryTask task, AlbumInput input, List<TrackInput> tracks, boolean cacheEnabled) {
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
        PreparationRequest request = new PreparationRequest(artistSearchPrompt,
                JsonConverter.toJson(Map.of("album", input, "tracks", tracks, "alternativeArtistNames", alternatives)),
                configService.get().llmModel(), 0.0, "none");
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, "MUSICBRAINZ_ALBUM_ARTIST_SEARCH", CACHE_VERSION) : null;
        return llmExecutor.call(task, request, ArtistNamesResponse.class, cacheSettings, response -> {
            try {
                ArtistNamesResponse data = RESPONSE_MAPPER.convertValue(
                        response != null ? LlmJsonConverter.fromJson(response) : null, ArtistNamesResponse.class);
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

    public @Nullable String selectReleaseGroup(
            DiscoveryTask task, AlbumInput input, MusicBrainzClient.ReleaseGroupSearchResult searchResult,
            List<TrackInput> tracks, List<String> searchArtistNames, boolean cacheEnabled
    ) {
        String trackContext = trackContext(tracks);
        String prompt = releaseGroupPrompt;
        if (!searchArtistNames.isEmpty()) {
            prompt += alternativeArtistInstructions;
            trackContext += "\nAlternative artist search names: " + JsonConverter.toJson(searchArtistNames);
        }
        Request request = new Request(prompt, context(input, searchResult.rawResponse(), false) + trackContext,
                context(input, searchResult.rawResponse(), true) + trackContext,
                configService.get().llmModel());
        Set<String> candidates = searchResult.groups().stream().map(MusicBrainzDto.ReleaseGroup::id).collect(Collectors.toSet());
        return selectId(task, input, request, candidates, "MUSICBRAINZ_ALBUM_DATA_RELEASE_GROUP",
                LlmStage.RELEASE_GROUP_SELECTION, cacheEnabled);
    }

    private String trackContext(List<TrackInput> tracks) {
        return "\nLibrary album track count: " + tracks.size() + "\nLibrary album tracks: " + JsonConverter.toJson(tracks);
    }

    private String context(AlbumInput input, String response, boolean forCache) {
        Map<String, Object> data = JsonConverter.mapFromJson(response, String.class, Object.class);
        if (forCache) {
            data.remove("created");
        }
        return "Album from the library: " + JsonConverter.toJson(input)
                + "\nMusicBrainz search response: " + JsonConverter.toJson(new SearchResponse(data));
    }

    private @Nullable String selectId(
            DiscoveryTask task, AlbumInput input, Request request, Set<String> candidates,
            String keyPrefix, LlmStage stage, boolean cacheEnabled
    ) {
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, keyPrefix, CACHE_VERSION) : null;
        return llmExecutor.call(task, request, IdResponse.class, cacheSettings, response -> {
            try {
                IdResponse data = RESPONSE_MAPPER.convertValue(
                        response != null ? LlmJsonConverter.fromJson(response) : null, IdResponse.class);
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

    public @Nullable String selectArtist(
            DiscoveryTask task, AlbumInput input, ReleaseGroupSelection selection, List<TrackInput> tracks, boolean cacheEnabled
    ) {
        MusicBrainzDto.ReleaseGroup group = selection.group();
        List<String> searchArtistNames = selection.searchArtistNames();
        List<String> ids = group.artistCredit().stream().map(credit -> credit.artist().id()).distinct().toList();
        String selectedGroup = "\nSelected release group ID: " + JsonConverter.toJson(group.id());
        String prompt = artistPrompt;
        if (!searchArtistNames.isEmpty()) {
            prompt += alternativeArtistInstructions;
            selectedGroup += "\nAlternative artist search names: " + JsonConverter.toJson(searchArtistNames) + trackContext(tracks);
        }
        Request request = new Request(prompt, context(input, selection.response(), false) + selectedGroup,
                context(input, selection.response(), true) + selectedGroup, configService.get().llmModel());
        return selectId(task, input, request, Set.copyOf(ids), "MUSICBRAINZ_ALBUM_DATA_ARTIST",
                LlmStage.ARTIST_SELECTION, cacheEnabled);
    }

    private enum LlmStage {
        ALBUM_TITLE_NORMALIZATION,
        ARTIST_SEARCH,
        RELEASE_GROUP_SELECTION,
        ARTIST_SELECTION
    }

    @JsonPropertyOrder({"title", "year", "artistName"})
    record AlbumInput(@Nullable String title, @Nullable Integer year, @Nullable String artistName) {
    }

    @JsonPropertyOrder({"title", "discNumber", "trackNumber", "durationSeconds", "artistName", "albumArtistName"})
    record TrackInput(
            @Nullable String title, @Nullable Integer discNumber, @Nullable Integer trackNumber, Long durationSeconds,
            @Nullable String artistName, @Nullable String albumArtistName
    ) {
    }

    record ReleaseGroupSelection(MusicBrainzDto.ReleaseGroup group, String response, List<String> searchArtistNames) {
    }

    private record SearchResponse(
            @JsonAnyGetter @JsonFormat(with = WRITE_SORTED_MAP_ENTRIES) Map<String, Object> data
    ) {
    }

    private record IdResponse(@JsonProperty(required = true) @Nullable String id) {
    }

    private record ArtistNamesResponse(List<String> artistNames) {
    }

    @JsonPropertyOrder({"searchTitle", "searchFragments", "alternativeTitles"})
    record NormalizationResponse(String searchTitle, List<String> searchFragments, List<String> alternativeTitles) {
    }

    @JsonPropertyOrder({"systemPrompt", "userPrompt", "model", "temperature", "toolChoice"})
    private record PreparationRequest(
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
