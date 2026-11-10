package net.dorokhov.pony2.core.library.service.discovery;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzArtistDataService;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyArtistDataService;
import net.dorokhov.pony2.core.library.service.discovery.spotify.SpotifyTopTracksService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import net.dorokhov.pony2.core.library.service.image.ExternalImageStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static net.dorokhov.pony2.api.library.domain.DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA;
import static net.dorokhov.pony2.api.library.domain.DiscoveryTaskType.MUSICBRAINZ_ARTIST_DATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

class MusicBrainzArtistDiscoveryIntegrationTest extends IntegrationTest {

    @Autowired private ConfigService configService;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private DiscoveryTaskRepository taskRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DiscoveryTaskResultReader resultReader;
    @Autowired private MusicBrainzArtistDataService musicBrainzArtistDataService;
    @Autowired private DiscoveryTaskExecutor taskExecutor;
    @Autowired private LibraryJobSynchronizer jobSynchronizer;
    @Autowired private ExternalImageStorage imageStorage;
    @Autowired private LogService logService;
    @Autowired private PlatformTransactionManager transactionManager;

    private final AlbumDiscoveryService albumDiscoveryService = mock(AlbumDiscoveryService.class);
    private ArtistDiscoveryService service;
    private LibraryJobSynchronizer.LibraryJobRegistration registration;
    private Artist artist;
    private Album album;
    private Instant originalAlbumImplementedAt;

    @BeforeEach
    void setUp() throws Exception {
        originalAlbumImplementedAt = MUSICBRAINZ_ALBUM_DATA.getImplementedAt().orElse(null);
        // Keep incremental-discovery scenarios independent of the production migration cutoff.
        ReflectionTestUtils.setField(MUSICBRAINZ_ALBUM_DATA, "implementedAt", Instant.EPOCH);
        configService.update(new ConfigSetUpdateCommand().setDiscoveryEnabled(true)
                .setLlmUrl("http://localhost:11434/v1").setLlmModel("test"));
        registration = jobSynchronizer.registerDiscoveryJob();
        artist = artistRepository.save(new Artist().setName("Nick Cave"));
        album = albumRepository.save(new Album().setArtist(artist).setName("Idiot Prayer").setYear(2020));
        service = service(artistDiscoveryRepository);
        DiscoveryJob history = job();
        for (DiscoveryTaskType type : DiscoveryTaskType.values()) {
            if (type != MUSICBRAINZ_ARTIST_DATA && type != MUSICBRAINZ_ALBUM_DATA) {
                taskRepository.save(new DiscoveryTask().setJob(history).setArtist(artist).setType(type).setStatus(DiscoveryTask.Status.COMPLETE));
            }
        }
        lenient().when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
    }

    private ArtistDiscoveryService service(ArtistDiscoveryRepository discoveryRepository) {
        return new ArtistDiscoveryService(discoveryRepository, albumRepository, taskRepository, resultReader,
                mock(SpotifyArtistDataService.class), mock(SpotifyTopTracksService.class), albumDiscoveryService,
                musicBrainzArtistDataService, imageStorage, mock(ArtistBioImageFacesService.class), logService,
                taskExecutor, jobSynchronizer, transactionManager);
    }

    @AfterEach
    void tearDown() {
        try {
            registration.close();
        } finally {
            ReflectionTestUtils.setField(MUSICBRAINZ_ALBUM_DATA, "implementedAt", originalAlbumImplementedAt);
        }
    }

    @Test
    void shouldSelectLibraryArtistInsteadOfEarlierRelatedBandWithoutHttpRequests() {
        Album related = albumRepository.save(new Album().setArtist(artist).setName("Burnin' the Ice").setYear(1983));
        albumResult(related, "die-haut", "Die Haut", List.of("DE"));
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            Prompt prompt = invocation.getArgument(0);
            assertThat(prompt.getInstructions().get(1).getText()).contains("Nick Cave", "Die Haut", "Burnin' the Ice", "Person");
            assertThat(artistDiscoveryRepository.findByArtistId(artist.getId())).isEmpty();
            return response("{\"id\":\"nick-cave\"}");
        });

        runUpdated(false);

        assertDiscovery("nick-cave", "AU");
        assertThat(latestTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(latestTask().getResult(), MusicBrainzArtistData.class))
                .isEqualTo(new MusicBrainzArtistData("nick-cave", "AU"));
        assertThat(latestTask().getRawRequests()).singleElement().satisfies(request -> {
            Map<String, Object> raw = JsonConverter.mapFromJson(JsonConverter.toJson(request), String.class, Object.class);
            assertThat(raw).containsKeys("systemPrompt", "userPrompt", "model", "responseSchema");
            assertThat(raw.get("userPrompt").toString()).contains("artistDisambiguation", "artistCreditNames");
        });
        verifyNoInteractions(musicBrainzClient, albumDiscoveryService);
    }

    @Test
    void shouldClearUnconfirmedArtistAndCountryEvenWithSingleCandidate() {
        saveDiscovery();
        albumResult(album, "die-haut", "Die Haut", List.of("DE"));
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":null}"));

        runUpdated(false);

        assertDiscovery(null, null);
        assertThat(latestTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(latestTask().getResult()).isNull();
        assertThat(latestTask().getRawResponses()).containsExactly(JsonConverter.fromJsonOrString("{\"id\":null}"));
        long tasks = taskRepository.count();
        runUpdated(false);
        assertDiscovery(null, null);
        assertThat(taskRepository.count()).isEqualTo(tasks);
        verify(model).call(any(Prompt.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSelectSingleMatchingArtistWithoutCallingLlm(boolean cacheEnabled) {
        albumResult(album, "nick-cave", "NICK CAVE", List.of("AU"), List.of());

        runUpdated(cacheEnabled);

        assertDiscovery("nick-cave", "AU");
        assertThat(latestTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(JsonConverter.fromJson(latestTask().getResult(), MusicBrainzArtistData.class))
                .isEqualTo(new MusicBrainzArtistData("nick-cave", "AU"));
        assertThat(latestTask().getRawRequests()).isNull();
        assertThat(latestTask().getRawResponses()).isNull();
        verifyNoInteractions(model, musicBrainzClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "Nicholas Edward Cave")
    void shouldSelectSingleArtistWithMatchingCreditWithoutCallingLlm(String artistName) {
        saveDiscovery();
        albumResult(album, "nick-cave", artistName, List.of(), List.of("NICK CAVE"));

        runUpdated(false);

        assertDiscovery("nick-cave", null);
        verifyNoInteractions(model, musicBrainzClient);
    }

    @Test
    void shouldUseLatestMetadataForSingleArtistAcrossAlbumsWithoutCallingLlm() {
        albumResult(album, "nick-cave", "Nick Cave", List.of("GB"));
        Album newer = albumRepository.save(new Album().setArtist(artist).setName("Seven Psalms"));
        albumResult(newer, "nick-cave", "Nick Cave", List.of("AU"));

        runUpdated(false);

        assertDiscovery("nick-cave", "AU");
        verifyNoInteractions(model, musicBrainzClient);
    }

    @Test
    void shouldClearCountryWhenSelectedArtistHasNoCountry() {
        saveDiscovery();
        albumResult(album, "nick-cave", "Nick Cave", List.of());
        Album related = albumRepository.save(new Album().setArtist(artist).setName("Related album"));
        albumResult(related, "die-haut", "Die Haut", List.of("DE"));
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":\"nick-cave\"}"));

        runUpdated(false);

        assertDiscovery("nick-cave", null);
        assertThat(latestTask().getResult()).isNotNull();
    }

    @Test
    void shouldClearDiscoveryWithoutCallingLlmWhenNoCandidatesExist() {
        saveDiscovery();
        albumRepository.deleteAll();

        runUpdated(false);

        assertDiscovery(null, null);
        assertThat(latestTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
        assertThat(latestTask().getRawRequests()).isNull();
        assertThat(latestTask().getRawResponses()).isNull();
        verifyNoInteractions(model, musicBrainzClient);
    }

    @Test
    void shouldUseLatestMetadataForSameArtistAndKeepAllAlbumContext() {
        albumResult(album, "nick-cave", "Nick Cave", List.of("GB"));
        Album newer = albumRepository.save(new Album().setArtist(artist).setName("Seven Psalms"));
        albumResult(newer, "nick-cave", "Nick Cave", List.of("AU"));
        relatedAlbumResult();
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            Map<String, Object> input = JsonConverter.mapFromJson(prompt.getInstructions().get(1).getText(), String.class, Object.class);
            assertThat((List<?>) input.get("candidates")).hasSize(2);
            assertThat(prompt.getInstructions().get(1).getText()).contains("Idiot Prayer", "Seven Psalms");
            return response("{\"id\":\"nick-cave\"}");
        });

        runUpdated(false);

        assertDiscovery("nick-cave", "AU");
    }

    private void relatedAlbumResult() {
        Album related = albumRepository.save(new Album().setArtist(artist).setName("Burnin' the Ice").setYear(1983));
        albumResult(related, "die-haut", "Die Haut", List.of("DE"));
    }

    @Test
    void shouldIgnoreOlderAlbumDataWhenLatestCompletedResultIsEmpty() {
        saveDiscovery();
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));
        DiscoveryTask emptyTask = taskRepository.save(new DiscoveryTask().setJob(job()).setAlbum(album).setType(MUSICBRAINZ_ALBUM_DATA)
                .setStatus(DiscoveryTask.Status.COMPLETE));
        jdbcTemplate.update("""
                UPDATE discovery_task SET creation_date = ?
                WHERE album_id = ? AND type = ? AND result IS NOT NULL
                """, Timestamp.valueOf(emptyTask.getCreationDate().minusNanos(1)), album.getId(), MUSICBRAINZ_ALBUM_DATA.name());

        runUpdated(false);

        assertDiscovery(null, null);
        verifyNoInteractions(model);
    }

    @ParameterizedTest
    @EnumSource(value = DiscoveryTask.Status.class, names = {"STARTED", "FAILED", "INTERRUPTED"})
    void shouldRecheckAfterNewAlbumAttemptAndUseItsLatestCompletedData(DiscoveryTask.Status status) {
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));
        runUpdated(false);
        DiscoveryTask previousTask = latestTask();
        await().until(() -> LocalDateTime.now().isAfter(previousTask.getCreationDate()));
        taskRepository.save(new DiscoveryTask().setJob(job()).setAlbum(album).setType(MUSICBRAINZ_ALBUM_DATA).setStatus(status));

        runUpdated(false);

        assertThat(latestTask().getId()).isNotEqualTo(previousTask.getId());
        assertDiscovery("nick-cave", "AU");
        verifyNoInteractions(model);
    }

    @Test
    void shouldRetryFailedCheckAndPreserveDiscoveryUntilSuccess() {
        saveDiscovery();
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));
        relatedAlbumResult();
        when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("LLM unavailable"));

        runUpdated(false);

        assertDiscovery("previous-artist", "DE");
        assertThat(latestTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
        assertThat(latestTask().getRawRequests()).hasSize(1);
        doReturn(response("{\"id\":\"nick-cave\"}")).when(model).call(any(Prompt.class));
        runUpdated(false);
        assertDiscovery("nick-cave", "AU");
        assertThat(latestTask().getStatus()).isEqualTo(DiscoveryTask.Status.COMPLETE);
    }

    @Test
    void shouldResumeInterruptedCheckWithoutPublishingPartialChanges() {
        saveDiscovery();
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));
        relatedAlbumResult();
        when(model.call(any(Prompt.class))).thenThrow(new DiscoveryInterruptedException());

        assertThatThrownBy(() -> runUpdated(false)).isInstanceOf(DiscoveryInterruptedException.class);

        assertDiscovery("previous-artist", "DE");
        assertThat(latestTask().getStatus()).isEqualTo(DiscoveryTask.Status.INTERRUPTED);
        doReturn(response("{\"id\":\"nick-cave\"}")).when(model).call(any(Prompt.class));
        runUpdated(false);
        assertDiscovery("nick-cave", "AU");
    }

    @Test
    void shouldRollBackCompletedTaskWhenUpdatingDiscoveryFails() {
        saveDiscovery();
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));
        ArtistDiscoveryRepository failingRepository = mock(ArtistDiscoveryRepository.class, delegatesTo(artistDiscoveryRepository));
        doThrow(new IllegalStateException("Cannot save discovery")).when(failingRepository).save(any(ArtistDiscovery.class));
        service = service(failingRepository);

        runUpdated(false);

        assertDiscovery("previous-artist", "DE");
        assertThat(latestTask().getStatus()).isEqualTo(DiscoveryTask.Status.FAILED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"id\":123}", "{\"id\":\"invented\"}", "not JSON"})
    void shouldRecordInvalidAnswersAndValidateFollowUp(String invalidResponse) {
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));
        relatedAlbumResult();
        when(model.call(any(Prompt.class))).thenReturn(response(invalidResponse), response("{\"id\":\"nick-cave\"}"));

        runUpdated(false);

        assertDiscovery("nick-cave", "AU");
        assertThat(latestTask().getRawRequests()).hasSize(2);
        assertThat(latestTask().getRawResponses()).hasSize(2);
    }

    @Test
    void shouldPreserveUnrelatedDataAndDatesWhenMusicBrainzValuesAreUnchanged() {
        ArtistDiscovery previous = artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(artist)
                .setMusicBrainzArtistId("nick-cave").setCountryCode("AU").setSpotifyArtistId("spotify-id"));
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));

        runUpdated(false);

        assertThat(artistDiscoveryRepository.findByArtistId(artist.getId())).get().satisfies(discovery -> {
            assertThat(discovery.getId()).isEqualTo(previous.getId());
            assertThat(discovery.getCreationDate()).isEqualTo(previous.getCreationDate());
            assertThat(discovery.getUpdateDate()).isEqualTo(previous.getUpdateDate());
            assertThat(discovery.getSpotifyArtistId()).isEqualTo("spotify-id");
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReuseLlmCacheAcrossModelAndEndpointChangesAndRecordBothExchanges(boolean cacheEnabled) throws Exception {
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));
        relatedAlbumResult();
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":\"nick-cave\"}"));

        service.discover(job(), artist, cacheEnabled, null);
        registration.close();
        configService.update(new ConfigSetUpdateCommand().setDiscoveryEnabled(true)
                .setLlmUrl("http://another-endpoint/v1").setLlmModel("different-model"));
        registration = jobSynchronizer.registerDiscoveryJob();
        service.discover(job(), artist, cacheEnabled, null);

        verify(model, times(cacheEnabled ? 1 : 2)).call(any(Prompt.class));
        List<DiscoveryTask> tasks = taskRepository.findAll().stream().filter(task -> task.getType() == MUSICBRAINZ_ARTIST_DATA).toList();
        assertThat(tasks).hasSize(2).allSatisfy(task -> {
            assertThat(task.getRawRequests()).hasSize(1);
            assertThat(task.getRawResponses()).containsExactly(JsonConverter.fromJsonOrString("{\"id\":\"nick-cave\"}"));
        });
        if (cacheEnabled) {
            assertThat(tasks.getFirst().getRawRequests()).isEqualTo(tasks.getLast().getRawRequests());
        } else {
            assertThat(tasks.getFirst().getRawRequests()).isNotEqualTo(tasks.getLast().getRawRequests());
        }
        verifyNoInteractions(musicBrainzClient);
    }

    @Test
    void shouldReuseCachedCheckWhenUnchangedAlbumIsRediscovered() {
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));
        Album other = albumRepository.save(new Album().setArtist(artist).setName("Seven Psalms").setYear(2022));
        albumResult(other, "nick-cave", "Nick Cave", List.of("AU"));
        relatedAlbumResult();
        when(model.call(any(Prompt.class))).thenReturn(response("{\"id\":\"nick-cave\"}"));
        runUpdated(true);
        String previousTaskId = latestTask().getId();
        albumResult(album, "nick-cave", "Nick Cave", List.of("AU"));

        runUpdated(true);

        assertThat(latestTask().getId()).isNotEqualTo(previousTaskId);
        assertDiscovery("nick-cave", "AU");
        verify(model).call(any(Prompt.class));
    }

    private void runUpdated(boolean cacheEnabled) {
        service.discoverIfUpdated(job(), artist, albumRepository.findByArtistId(artist.getId()), cacheEnabled, null);
    }

    private void albumResult(Album target, String artistId, String artistName, List<String> country) {
        albumResult(target, artistId, artistName, country, List.of(artistName));
    }

    private void albumResult(Album target, String artistId, String artistName, List<String> country, List<String> creditNames) {
        taskRepository.save(new DiscoveryTask().setJob(job()).setAlbum(target).setType(MUSICBRAINZ_ALBUM_DATA)
                .setStatus(DiscoveryTask.Status.COMPLETE).setResult(JsonConverter.toJson(new MusicBrainzAlbumData(
                        "group-" + target.getId(), "Album", List.of(), null, artistId, country, artistName,
                        "Artist description", "nick-cave".equals(artistId) ? "Person" : "Group", creditNames))));
    }

    private DiscoveryJob job() {
        return jobRepository.save(new DiscoveryJob().setType(DiscoveryType.FULL).setStatus(DiscoveryJob.Status.STARTED));
    }

    private void saveDiscovery() {
        artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(artist).setSpotifyArtistId("spotify-id")
                .setMusicBrainzArtistId("previous-artist").setCountryCode("DE"));
    }

    private void assertDiscovery(String artistId, String countryCode) {
        assertThat(artistDiscoveryRepository.findByArtistId(artist.getId())).get().satisfies(discovery -> {
            assertThat(discovery.getMusicBrainzArtistId()).isEqualTo(artistId);
            assertThat(discovery.getCountryCode()).isEqualTo(countryCode);
        });
    }

    private DiscoveryTask latestTask() {
        String taskId = taskRepository.findByArtistId(artist.getId(), MUSICBRAINZ_ARTIST_DATA,
                false, false, PageRequest.of(0, 1)).getContent().getFirst().id();
        return taskRepository.findById(taskId).orElseThrow();
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
