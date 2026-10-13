package net.dorokhov.pony2.core.library.service.discovery.task;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
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
import net.dorokhov.pony2.core.library.service.exception.FollowUpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
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

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.llm.domain.DiscoveryCacheRegion.MUSICBRAINZ;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class MusicBrainzAlbumDataService {

    private static final int CACHE_VERSION = 1;

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
            @Value("classpath:prompts/musicbrainz-verification.txt") Resource verificationPrompt
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
        String responseFormat = new BeanOutputConverter<>(IdResponse.class).getFormat();
        this.releaseGroupPrompt = releaseGroupPrompt.getContentAsString(UTF_8) + "\n" + responseFormat;
        this.artistPrompt = artistPrompt.getContentAsString(UTF_8) + "\n" + responseFormat;
        this.verificationPrompt = verificationPrompt.getContentAsString(UTF_8);
    }

    @Nullable
    public MusicBrainzAlbumData discover(DiscoveryTask task, boolean cacheEnabled) {
        interruptIfNeeded();
        String albumId = JsonConverter.fromJson(task.getParameter(), DiscoveryTask.AlbumParameter.class).albumId();
        AlbumInput input = requireNonNull(transactionTemplate.execute(status -> {
            Album album = albumRepository.findById(albumId).orElseThrow();
            return new AlbumInput(album.getName(), album.getYear(), album.getArtist().getName());
        }));
        if (input.title() == null || input.title().isBlank() || input.artistName() == null || input.artistName().isBlank()) {
            logger.debug("Skipping MusicBrainz discovery for album '{}': album title or artist name is unavailable.", albumId);
            return null;
        }
        String userAgent = "pony2/" + buildVersionProvider.getBuildVersion().getVersion() + " (" + userService.getMainAdmin().getEmail() + ")";
        List<String> searchResponses = new ArrayList<>();
        MusicBrainzClient.ExchangeObserver observer = new MusicBrainzClient.ExchangeObserver() {
            @Override
            public void onRequest(String url) {
                interruptIfNeeded();
                saveExchange(task, url, null);
            }

            @Override
            public void onResponse(String url, String response) {
                saveExchange(task, url, response);
                if (url.contains("/release-group?")) {
                    searchResponses.add(response);
                }
            }
        };
        try (
                MusicBrainzClient.Session session = client.openSession(userAgent, observer, cacheEnabled);
                LibraryJobSynchronizer.CancellationSubscription ignored = jobSynchronizer.onCancel(session::cancel)
        ) {
            String query = "release:\"" + escapeQuery(input.title()) + "\" AND artistname:\"" + escapeQuery(input.artistName()) + "\"";
            List<MusicBrainzDto.ReleaseGroup> groups = session.searchReleaseGroups(query);
            interruptIfNeeded();
            MusicBrainzDto.ReleaseGroup group = selectReleaseGroup(task, input, groups, searchResponses, cacheEnabled);
            if (group == null) {
                logger.debug("No unambiguous MusicBrainz release group was selected for album '{}'.", albumId);
                return null;
            }
            String artistId = selectArtist(task, input, group, searchResponses, cacheEnabled);
            if (artistId == null) {
                logger.debug("No corresponding MusicBrainz artist was selected for album '{}' in release group '{}'.", albumId, group.id());
                return null;
            }
            MusicBrainzDto.Artist artist = session.getArtist(artistId);
            interruptIfNeeded();
            MusicBrainzAlbumData result = new MusicBrainzAlbumData(group.id(), group.primaryType(), group.secondaryTypes(), group.firstReleaseDate(),
                    artist.id(), artist.area() != null ? artist.area().country() : List.of());
            logger.debug("MusicBrainz discovery result for album '{}': {}.", albumId, result);
            return result;
        } catch (MusicBrainzClient.CancelledException e) {
            throw new DiscoveryInterruptedException();
        }
    }

    @Nullable
    private MusicBrainzDto.ReleaseGroup selectReleaseGroup(
            DiscoveryTask task, AlbumInput input,
            List<MusicBrainzDto.ReleaseGroup> groups, List<String> responses,
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
        if (candidates.isEmpty() || !configService.get().llmEnabled()) {
            return null;
        }
        String albumId = JsonConverter.fromJson(task.getParameter(), DiscoveryTask.AlbumParameter.class).albumId();
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
        Request request = new Request(releaseGroupPrompt, context(input, responses, false) + trackContext,
                context(input, responses, true) + trackContext,
                configService.get().llmModel());
        String id = selectId(task, request, candidates.keySet(), "MUSICBRAINZ_ALBUM_DATA_RELEASE_GROUP", cacheEnabled);
        return id != null ? candidates.get(id) : null;
    }

    @Nullable
    private String selectArtist(
            DiscoveryTask task, AlbumInput input, MusicBrainzDto.ReleaseGroup group,
            List<String> responses, boolean cacheEnabled
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
        if (ids.isEmpty() || !configService.get().llmEnabled()) {
            return null;
        }
        String selectedGroup = "\nSelected release group ID: " + JsonConverter.toJson(group.id());
        Request request = new Request(artistPrompt, context(input, responses, false) + selectedGroup,
                context(input, responses, true) + selectedGroup, configService.get().llmModel());
        return selectId(task, request, Set.copyOf(ids), "MUSICBRAINZ_ALBUM_DATA_ARTIST", cacheEnabled);
    }

    @Nullable
    private String selectId(DiscoveryTask task, Request request, Set<String> candidates, String keyPrefix, boolean cacheEnabled) {
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, keyPrefix, CACHE_VERSION) : null;
        return llmExecutor.call(task, request, cacheSettings, response -> {
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
                throw invalidResponse(task, keyPrefix, e);
            }
        });
    }

    private FollowUpException invalidResponse(DiscoveryTask task, String selection, RuntimeException error) {
        String albumId = JsonConverter.fromJson(task.getParameter(), DiscoveryTask.AlbumParameter.class).albumId();
        logService.warn(logger, "Invalid MusicBrainz response from the LLM for album '{}' during '{}': {}",
                albumId, selection, error.getMessage());
        return new FollowUpException(verificationPrompt.formatted(JsonConverter.toJson(error.getMessage())), error);
    }

    private String context(AlbumInput input, List<String> responses, boolean forCache) {
        return "Album from the library: " + JsonConverter.toJson(input)
                + "\nComplete MusicBrainz search responses: " + JsonConverter.toJson(responses.stream()
                .map(response -> {
                    Map<String, Object> data = JsonConverter.mapFromJson(response, String.class, Object.class);
                    if (forCache) {
                        data.remove("created");
                    }
                    return new SearchResponse(data);
                }).toList());
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

    private record IdResponse(@JsonProperty(required = true) @Schema(nullable = true) @Nullable String id) {
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
