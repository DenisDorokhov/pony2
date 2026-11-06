package net.dorokhov.pony2.core.library.service.discovery.musicbrainz;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.common.LlmJsonConverter;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryTaskLlmExecutor;
import net.dorokhov.pony2.core.library.service.exception.LlmFollowUpException;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.DiscoveryCacheRegion.MUSICBRAINZ;
import static net.dorokhov.pony2.api.library.domain.DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA;

@Service
public class MusicBrainzArtistDataService {

    private static final JsonMapper RESPONSE_MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final DiscoveryTaskRepository taskRepository;
    private final DiscoveryTaskLlmExecutor llmExecutor;
    private final ConfigService configService;
    private final TransactionTemplate transactionTemplate;
    private final String prompt;
    private final String verificationPrompt;

    public MusicBrainzArtistDataService(
            AlbumRepository albumRepository, ArtistRepository artistRepository, DiscoveryTaskRepository taskRepository,
            DiscoveryTaskLlmExecutor llmExecutor, ConfigService configService, PlatformTransactionManager transactionManager,
            @Value("classpath:prompts/musicbrainz-artist-data.txt") Resource prompt,
            @Value("classpath:prompts/musicbrainz-verification.txt") Resource verificationPrompt
    ) throws IOException {
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.taskRepository = taskRepository;
        this.llmExecutor = llmExecutor;
        this.configService = configService;
        this.prompt = prompt.getContentAsString(UTF_8);
        this.verificationPrompt = verificationPrompt.getContentAsString(UTF_8);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public @Nullable MusicBrainzArtistData discover(DiscoveryTask task, boolean cacheEnabled) {
        Input input = requireNonNull(transactionTemplate.execute(status -> loadInput(requireNonNull(task.getArtist()).getId())));
        if (input.artistName() == null || input.artistName().isBlank() || input.candidates().isEmpty()) {
            return null;
        }
        Request request = new Request(prompt, JsonConverter.toJson(input), configService.get().llmModel());
        DiscoveryTaskLlmExecutor.CacheSettings cacheSettings = cacheEnabled
                ? new DiscoveryTaskLlmExecutor.CacheSettings(MUSICBRAINZ, "MUSICBRAINZ_ARTIST_DATA", 1) : null;
        String selectedId = llmExecutor.call(task, request, IdResponse.class, cacheSettings, response -> {
            try {
                IdResponse data = RESPONSE_MAPPER.convertValue(
                        response != null ? LlmJsonConverter.fromJson(response) : null, IdResponse.class);
                checkState(data != null, "The response must be a JSON object containing the id field.");
                checkState(data.id() == null || input.candidates().stream().anyMatch(candidate -> candidate.artistId().equals(data.id())),
                        "Choose one of the supplied candidate IDs or null.");
                return data.id();
            } catch (JacksonException | IllegalStateException e) {
                throw new LlmFollowUpException(verificationPrompt.formatted(JsonConverter.toJson(e.getMessage())), e);
            }
        });
        return input.candidates().stream().filter(candidate -> candidate.artistId().equals(selectedId)).findFirst()
                .map(candidate -> new MusicBrainzArtistData(candidate.artistId(),
                        candidate.country().isEmpty() ? null : candidate.country().getFirst()))
                .orElse(null);
    }

    private Input loadInput(String artistId) {
        Artist artist = artistRepository.findById(artistId).orElseThrow();
        Map<String, Album> albums = new LinkedHashMap<>();
        List<DiscoveryTask> tasks = new ArrayList<>();
        for (Album album : albumRepository.findByArtistId(artistId).stream().sorted().toList()) {
            albums.put(album.getId(), album);
            taskRepository.findLatestCompletedByAlbumIdAndType(album.getId(), MUSICBRAINZ_ALBUM_DATA, Limit.of(1))
                    .filter(task -> task.getResult() != null).ifPresent(tasks::add);
        }
        tasks.sort(Comparator.comparing(DiscoveryTask::getCreationDate).thenComparing(DiscoveryTask::getId).reversed());
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        for (DiscoveryTask task : tasks) {
            MusicBrainzAlbumData data = JsonConverter.fromJson(task.getResult(), MusicBrainzAlbumData.class);
            List<String> creditNames = data.artistCreditNames() != null ? data.artistCreditNames() : List.of();
            if ((data.artistName() == null || data.artistName().isBlank()) && creditNames.isEmpty()) {
                continue;
            }
            Candidate candidate = candidates.computeIfAbsent(data.artistId(), id -> new Candidate(id, data.artistName(),
                    data.artistDisambiguation(), data.artistType(), data.country(), new ArrayList<>()));
            Album album = albums.get(requireNonNull(task.getAlbum()).getId());
            candidate.albums().add(new AlbumContext(album.getName(), album.getYear(), creditNames));
        }
        for (Candidate candidate : candidates.values()) {
            candidate.albums().sort(Comparator.comparing(AlbumContext::title, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(AlbumContext::year, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return new Input(artist.getName(), candidates.values().stream().sorted(Comparator.comparing(Candidate::artistId)).toList());
    }

    private record Input(@Nullable String artistName, List<Candidate> candidates) {}

    private record Candidate(
            String artistId, @Nullable String artistName, @Nullable String artistDisambiguation,
            @Nullable String artistType, List<String> country, List<AlbumContext> albums
    ) {}

    private record AlbumContext(@Nullable String title, @Nullable Integer year, List<String> artistCreditNames) {}

    private record IdResponse(@JsonProperty(required = true) @Nullable String id) {}

    private record Request(String systemPrompt, String userPrompt, String model) implements DiscoveryTaskLlmExecutor.Request {
        @Override
        public Prompt toPrompt() {
            return new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                    OpenAiChatOptions.builder().model(model).toolChoice("none").build());
        }
    }
}
