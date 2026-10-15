package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.common.JacksonConfig;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.web.dto.*;
import net.dorokhov.pony2.web.service.DiscoveryFacade;
import net.dorokhov.pony2.web.service.LibraryFacade;
import net.dorokhov.pony2.web.service.ScanFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static net.dorokhov.pony2.test.DiscoveryJobFixtures.discoveryJob;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class LibraryDiscoveryAdminControllerTest {

    @Mock
    private DiscoveryJobService discoveryJobService;
    @Mock
    private LibraryService libraryService;
    @Mock
    private ScanFacade scanFacade;
    @Mock
    private LibraryFacade libraryFacade;

    private MockMvc mockMvc;
    private JsonMapper objectMapper;

    @BeforeEach
    public void setUp() {
        DiscoveryFacade discoveryFacade = new DiscoveryFacade(discoveryJobService, libraryService);
        JsonMapper.Builder mapperBuilder = JsonMapper.builder();
        new JacksonConfig().jsonMapperBuilderCustomizer().customize(mapperBuilder);
        objectMapper = mapperBuilder.build();
        mockMvc = MockMvcBuilders.standaloneSetup(new LibraryAdminController(scanFacade, discoveryFacade, libraryFacade))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .setControllerAdvice(new LibraryAdminController.Advice(), new ErrorHandlingController.Advice())
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "FULL, , true", "ARTIST, , true", "ALBUM, , true",
            "FULL, true, true", "ARTIST, true, true", "ALBUM, true, true",
            "FULL, false, false", "ARTIST, false, false", "ALBUM, false, false"
    })
    public void shouldStartDiscoveryJob(DiscoveryType type, String cacheEnabledParameter, boolean cacheEnabled) throws Exception {
        DiscoveryJob job = discoveryJob(type)
                .setId("job1")
                .setParameter(switch (type) {
                    case FULL -> null;
                    case ARTIST -> "artistId";
                    case ALBUM -> "albumId";
                })
                .setLogMessage(new LogMessage().setId("log1").setText("Starting discovery."));
        when(startJob(type, cacheEnabled)).thenReturn(job);

        MockHttpServletRequestBuilder request = post(startJobPath(type));
        if (cacheEnabledParameter != null) {
            request.param("cacheEnabled", cacheEnabledParameter);
        }
        String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DiscoveryJobDto dto = objectMapper.readValue(response, DiscoveryJobDto.class);

        assertThat(dto).satisfies(discoveryJob -> {
            assertThat(discoveryJob.getId()).isEqualTo("job1");
            assertThat(discoveryJob.getCreationDate()).isNotNull();
            assertThat(discoveryJob.getDiscoveryType()).isSameAs(type);
            assertThat(discoveryJob.getStatus()).isSameAs(DiscoveryJob.Status.STARTING);
            assertThat(discoveryJob.getParameter()).isEqualTo(job.getParameter());
            assertThat(discoveryJob.getLogMessage()).satisfies(logMessage -> {
                assertThat(logMessage.getId()).isEqualTo("log1");
                assertThat(logMessage.getText()).isEqualTo("Starting discovery.");
            });
        });

        switch (type) {
            case FULL -> verify(discoveryJobService).startFullJob(cacheEnabled);
            case ARTIST -> verify(discoveryJobService).startArtistJob("artistId", cacheEnabled);
            case ALBUM -> verify(discoveryJobService).startAlbumJob("albumId", cacheEnabled);
        }
    }

    @ParameterizedTest
    @EnumSource(DiscoveryType.class)
    public void shouldRejectConcurrentDiscoveryJob(DiscoveryType type) throws Exception {
        when(startJob(type, true)).thenThrow(new ConcurrentLibraryJobException());

        String response = mockMvc.perform(post(startJobPath(type)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        ErrorDto dto = objectMapper.readValue(response, ErrorDto.class);

        assertThat(dto.getCode()).isSameAs(ErrorDto.Code.CONCURRENT_LIBRARY_JOB);
        assertThat(dto.getMessage()).isEqualTo("Library job is already running.");
    }

    @ParameterizedTest
    @CsvSource({"artist, Artist", "album, Album"})
    public void shouldRejectUnknownDiscoveryTarget(String target, String objectType) throws Exception {
        String response = mockMvc.perform(post("/api/admin/library/discoveryJobs/" + target + "/missing"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        ErrorDto dto = objectMapper.readValue(response, ErrorDto.class);

        assertThat(dto).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments()).containsExactly(objectType, "missing");
        });

        verifyNoInteractions(discoveryJobService);
    }

    @Test
    public void shouldGetCompletedDiscoveryJob() throws Exception {
        DiscoveryJob job = completedJob();
        when(discoveryJobService.getById("job1")).thenReturn(Optional.of(job));

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobs/job1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(response).has("discoveryResult")).isFalse();
        DiscoveryJobDto dto = objectMapper.readValue(response, DiscoveryJobDto.class);

        assertThat(dto).satisfies(discoveryJob -> {
            assertThat(discoveryJob.getId()).isEqualTo("job1");
            assertThat(discoveryJob.getDiscoveryType()).isSameAs(DiscoveryType.FULL);
            assertThat(discoveryJob.getStatus()).isSameAs(DiscoveryJob.Status.MODERATE);
            assertThat(discoveryJob.getUpdateDate()).isNotNull();
        });
    }

    @Test
    public void shouldReturnNotFoundForUnknownDiscoveryJob() throws Exception {
        String response = mockMvc.perform(get("/api/admin/library/discoveryJobs/missing"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        ErrorDto dto = objectMapper.readValue(response, ErrorDto.class);

        assertThat(dto).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments()).containsExactly("DiscoveryJob", "missing");
        });
    }

    @ParameterizedTest
    @CsvSource({"0, 30, 30", "2, 5, 5", "0, 100, 30"})
    public void shouldGetDiscoveryJobsWithPagination(int pageIndex, int requestedSize, int pageSize) throws Exception {
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "creationDate", "updateDate"));
        when(discoveryJobService.getAll(any())).thenReturn(new PageImpl<>(List.of(completedJob()), pageRequest, 100));

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobs")
                        .param("pageIndex", String.valueOf(pageIndex))
                        .param("pageSize", String.valueOf(requestedSize)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DiscoveryJobPageDto dto = objectMapper.readValue(response, DiscoveryJobPageDto.class);

        assertThat(dto).satisfies(page -> {
            assertThat(page.getPageIndex()).isEqualTo(pageIndex);
            assertThat(page.getPageSize()).isEqualTo(pageSize);
            assertThat(page.getTotalPages()).isEqualTo((100 + pageSize - 1) / pageSize);
            assertThat(page.getDiscoveryJobs()).singleElement().satisfies(discoveryJob -> {
                assertThat(discoveryJob.getId()).isEqualTo("job1");
            });
        });

        verify(discoveryJobService).getAll(pageRequest);
    }

    @Test
    public void shouldUseDefaultPagination() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "creationDate", "updateDate"));
        when(discoveryJobService.getAll(any())).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DiscoveryJobPageDto dto = objectMapper.readValue(response, DiscoveryJobPageDto.class);

        assertThat(dto).satisfies(page -> assertThat(page.getDiscoveryJobs()).isEmpty());

        verify(discoveryJobService).getAll(pageRequest);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/job1"})
    public void shouldGetDiscoveryJobProgress(String suffix) throws Exception {
        DiscoveryJobProgress progress = new DiscoveryJobProgress(discoveryJob(DiscoveryType.FULL).setId("job1"),
                new DiscoveryProgress(DiscoveryProgress.Step.FULL_DISCOVERY, DiscoveryProgress.Value.of(3, 10)));
        mockProgress(suffix, progress);

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobProgress" + suffix))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        OptionalResponseDto<DiscoveryJobProgressDto> dto = objectMapper.readValue(response, new TypeReference<>() {});

        assertThat(dto).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isTrue();
            assertThat(optionalResponse.getValue()).satisfies(discoveryJobProgress -> {
                assertThat(discoveryJobProgress.getDiscoveryJob().getId()).isEqualTo("job1");
                assertThat(discoveryJobProgress.getDiscoveryProgress()).satisfies(discoveryProgress -> {
                    assertThat(discoveryProgress.getStepDescriptor()).satisfies(stepDescriptor -> {
                        assertThat(stepDescriptor.getStep()).isSameAs(DiscoveryProgress.Step.FULL_DISCOVERY);
                        assertThat(stepDescriptor.getDiscoveryType()).isSameAs(DiscoveryType.FULL);
                        assertThat(stepDescriptor.getStepNumber()).isEqualTo(0);
                        assertThat(stepDescriptor.getTotalSteps()).isEqualTo(1);
                    });
                    assertThat(discoveryProgress.getValue()).satisfies(value -> {
                        assertThat(value.getItemsComplete()).isEqualTo(3);
                        assertThat(value.getItemsTotal()).isEqualTo(10);
                    });
                });
            });
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/missing"})
    public void shouldReturnEmptyDiscoveryJobProgress(String suffix) throws Exception {
        String response = mockMvc.perform(get("/api/admin/library/discoveryJobProgress" + suffix))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        OptionalResponseDto<DiscoveryJobProgressDto> dto = objectMapper.readValue(response, new TypeReference<>() {});

        assertThat(dto).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isFalse();
            assertThat(optionalResponse.getValue()).isNull();
        });
    }

    @Test
    public void shouldGetDiscoveryJobProgressBeforeFirstStep() throws Exception {
        mockProgress("", new DiscoveryJobProgress(discoveryJob(DiscoveryType.FULL).setId("job1"), null));

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobProgress"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        OptionalResponseDto<DiscoveryJobProgressDto> dto = objectMapper.readValue(response, new TypeReference<>() {});

        assertThat(dto).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isTrue();
            assertThat(optionalResponse.getValue()).satisfies(discoveryJobProgress -> {
                assertThat(discoveryJobProgress.getDiscoveryJob().getId()).isEqualTo("job1");
                assertThat(discoveryJobProgress.getDiscoveryProgress()).isNull();
            });
        });
    }

    @Test
    public void shouldGetDiscoveryStepWithoutCounters() throws Exception {
        mockProgress("/job1", new DiscoveryJobProgress(discoveryJob(DiscoveryType.ARTIST).setId("job1"),
                new DiscoveryProgress(DiscoveryProgress.Step.ARTIST_DISCOVERY, null)));

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobProgress/job1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        OptionalResponseDto<DiscoveryJobProgressDto> dto = objectMapper.readValue(response, new TypeReference<>() {});

        assertThat(dto).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isTrue();
            assertThat(optionalResponse.getValue()).satisfies(discoveryJobProgress ->
                    assertThat(discoveryJobProgress.getDiscoveryProgress()).satisfies(discoveryProgress -> {
                        assertThat(discoveryProgress.getStepDescriptor().getStep()).isSameAs(DiscoveryProgress.Step.ARTIST_DISCOVERY);
                        assertThat(discoveryProgress.getValue()).isNull();
                    }));
        });
    }

    @ParameterizedTest
    @CsvSource({"STARTED, 587", "COMPLETE, 0"})
    public void shouldGetDiscoveryJobDetails(DiscoveryJob.Status jobStatus, long taskCount) throws Exception {
        when(discoveryJobService.getById("job1")).thenReturn(Optional.of(completedJob().setStatus(jobStatus)));
        when(discoveryJobService.getTaskCount("job1")).thenReturn(taskCount);

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobs/job1/details"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DiscoveryJobDetailsDto details = objectMapper.readValue(response, DiscoveryJobDetailsDto.class);

        assertThat(details.getDiscoveryJob().getId()).isEqualTo("job1");
        assertThat(details.getDiscoveryJob().getStatus()).isSameAs(jobStatus);
        assertThat(details.getTaskCount()).isEqualTo(taskCount);
    }

    @ParameterizedTest
    @EnumSource(DiscoveryTaskType.class)
    public void shouldGetDiscoveryTasksWithTargets(DiscoveryTaskType type) throws Exception {
        Artist artist = new Artist().setId("artist1").setName("Artist");
        Album album = new Album().setId("album1").setName("Album").setYear(2001).setArtist(artist);
        if (type == DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA) {
            when(libraryService.getAlbumById("album1")).thenReturn(Optional.of(album));
        } else {
            when(libraryService.getArtistById("artist1")).thenReturn(Optional.of(artist));
        }
        DiscoveryTask task = task(type, DiscoveryTask.Status.COMPLETE);
        PageRequest pageRequest = PageRequest.of(2, 5);
        when(discoveryJobService.getById("job1")).thenReturn(Optional.of(completedJob()));
        when(discoveryJobService.getTasks("job1", type, pageRequest))
                .thenReturn(new PageImpl<>(List.of(task), pageRequest, 21));

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobs/job1/tasks")
                        .param("type", type.name()).param("pageIndex", "2").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DiscoveryTaskPageDto page = objectMapper.readValue(response, DiscoveryTaskPageDto.class);

        assertThat(page.getPageIndex()).isEqualTo(2);
        assertThat(page.getPageSize()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(5);
        assertThat(page.getDiscoveryTasks()).singleElement().satisfies(summary -> {
            assertThat(summary.getId()).isEqualTo(task.getId());
            assertThat(summary.getCreationDate()).isEqualTo(task.getCreationDate());
            assertThat(summary.getUpdateDate()).isEqualTo(task.getUpdateDate());
            assertThat(summary.getType()).isSameAs(type);
            assertThat(summary.getStatus()).isSameAs(DiscoveryTask.Status.COMPLETE);
            assertThat(summary.getDuration()).isEqualTo(900L);
            assertThat(summary.getArtist().getName()).isEqualTo("Artist");
            if (type == DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA) {
                assertThat(summary.getTargetId()).isEqualTo("album1");
                assertThat(summary.getAlbum().getName()).isEqualTo("Album");
                assertThat(summary.getAlbum().getYear()).isEqualTo(2001);
            } else {
                assertThat(summary.getTargetId()).isEqualTo("artist1");
                assertThat(summary.getAlbum()).isNull();
            }
        });
        assertThat(objectMapper.readTree(response).get("discoveryTasks").get(0).has("rawRequest")).isFalse();
        assertThat(objectMapper.readTree(response).get("discoveryTasks").get(0).has("parameter")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(DiscoveryTaskType.class)
    public void shouldKeepTasksOfDeletedTargets(DiscoveryTaskType type) throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 30);
        when(discoveryJobService.getById("job1")).thenReturn(Optional.of(completedJob()));
        when(discoveryJobService.getTasks("job1", null, pageRequest))
                .thenReturn(new PageImpl<>(List.of(task(type, DiscoveryTask.Status.FAILED)), pageRequest, 1));

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobs/job1/tasks"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DiscoveryTaskPageDto page = objectMapper.readValue(response, DiscoveryTaskPageDto.class);

        assertThat(page.getDiscoveryTasks()).singleElement().satisfies(summary -> {
            assertThat(summary.getTargetId()).isEqualTo(type == DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA ? "album1" : "artist1");
            assertThat(summary.getArtist()).isNull();
            assertThat(summary.getAlbum()).isNull();
            assertThat(summary.getStatus()).isSameAs(DiscoveryTask.Status.FAILED);
        });
    }

    @ParameterizedTest
    @CsvSource({"0, 30", "100, 30"})
    public void shouldGetEmptyDiscoveryTasksAndLimitPageSize(int requestedSize, int pageSize) throws Exception {
        PageRequest pageRequest = PageRequest.of(0, pageSize);
        when(discoveryJobService.getById("job1")).thenReturn(Optional.of(completedJob()));
        when(discoveryJobService.getTasks("job1", null, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));
        MockHttpServletRequestBuilder request = get("/api/admin/library/discoveryJobs/job1/tasks");
        if (requestedSize > 0) {
            request.param("pageSize", String.valueOf(requestedSize));
        }

        String response = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DiscoveryTaskPageDto page = objectMapper.readValue(response, DiscoveryTaskPageDto.class);

        assertThat(page.getPageIndex()).isZero();
        assertThat(page.getPageSize()).isEqualTo(30);
        assertThat(page.getTotalPages()).isZero();
        assertThat(page.getDiscoveryTasks()).isEmpty();
    }

    @Test
    public void shouldUseCurrentTimeForRunningTaskDuration() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 30);
        DiscoveryTask task = task(DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.STARTED)
                .setUpdateDate(LocalDateTime.now().minusMinutes(10));
        when(discoveryJobService.getById("job1")).thenReturn(Optional.of(completedJob().setStatus(DiscoveryJob.Status.STARTED)));
        when(discoveryJobService.getTasks("job1", null, pageRequest))
                .thenReturn(new PageImpl<>(List.of(task), pageRequest, 1));

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobs/job1/tasks"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readValue(response, DiscoveryTaskPageDto.class).getDiscoveryTasks().getFirst().getDuration())
                .isBetween(900L, 905L);
    }

    @Test
    public void shouldLeaveDurationUnknownWithoutEndDate() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 30);
        when(discoveryJobService.getById("job1")).thenReturn(Optional.of(completedJob()));
        when(discoveryJobService.getTasks("job1", null, pageRequest))
                .thenReturn(new PageImpl<>(List.of(task(DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.INTERRUPTED)
                        .setUpdateDate(null)), pageRequest, 1));

        String response = mockMvc.perform(get("/api/admin/library/discoveryJobs/job1/tasks"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readValue(response, DiscoveryTaskPageDto.class).getDiscoveryTasks().getFirst().getDuration()).isNull();
    }

    @ParameterizedTest
    @EnumSource(DiscoveryTask.Status.class)
    public void shouldGetFullDiscoveryTask(DiscoveryTask.Status taskStatus) throws Exception {
        DiscoveryTask task = task(DiscoveryTaskType.SPOTIFY_TOP_TRACKS, taskStatus)
                .setResult("{\"tracks\":[]}").setRawRequest("request\nline two").setRawResult("response\nline two");
        when(discoveryJobService.getTaskById("task1")).thenReturn(Optional.of(task));

        String response = mockMvc.perform(get("/api/admin/library/discoveryTasks/task1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DiscoveryTaskDto dto = objectMapper.readValue(response, DiscoveryTaskDto.class);

        assertThat(dto.getId()).isEqualTo("task1");
        assertThat(dto.getCreationDate()).isEqualTo(task.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(task.getUpdateDate());
        assertThat(dto.getStatus()).isSameAs(taskStatus);
        assertThat(dto.getType()).isSameAs(task.getType());
        assertThat(dto.getParameter()).isEqualTo(task.getParameter());
        assertThat(dto.getResult()).isEqualTo(task.getResult());
        assertThat(dto.getRawRequest()).isEqualTo(task.getRawRequest());
        assertThat(dto.getRawResult()).isEqualTo(task.getRawResult());
        verifyNoInteractions(libraryService);
    }

    @Test
    public void shouldGetTaskWithoutOptionalFields() throws Exception {
        when(discoveryJobService.getTaskById("task1"))
                .thenReturn(Optional.of(task(DiscoveryTaskType.SPOTIFY_ARTIST_DATA, DiscoveryTask.Status.STARTED).setUpdateDate(null)));

        String response = mockMvc.perform(get("/api/admin/library/discoveryTasks/task1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DiscoveryTaskDto dto = objectMapper.readValue(response, DiscoveryTaskDto.class);

        assertThat(dto.getUpdateDate()).isNull();
        assertThat(dto.getResult()).isNull();
        assertThat(dto.getRawRequest()).isNull();
        assertThat(dto.getRawResult()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/discoveryJobs/missing/details", "/discoveryJobs/missing/tasks", "/discoveryTasks/missing"})
    public void shouldReturnNotFoundForUnknownJobOrTask(String path) throws Exception {
        String response = mockMvc.perform(get("/api/admin/library" + path))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        ErrorDto error = objectMapper.readValue(response, ErrorDto.class);

        assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
        assertThat(error.getArguments()).containsExactly(path.startsWith("/discoveryTasks/") ? "DiscoveryTask" : "DiscoveryJob", "missing");
    }

    @Test
    public void shouldRejectUnknownTaskType() throws Exception {
        mockMvc.perform(get("/api/admin/library/discoveryJobs/job1/tasks").param("type", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(discoveryJobService);
    }

    private DiscoveryTask task(DiscoveryTaskType type, DiscoveryTask.Status status) {
        LocalDateTime endDate = LocalDateTime.now();
        Object parameter = switch (type) {
            case SPOTIFY_ARTIST_DATA -> new DiscoveryTask.ArtistParameter("artist1");
            case SPOTIFY_TOP_TRACKS -> new DiscoveryTask.SpotifyTopTracksParameter("artist1", "dataTask1");
            case MUSICBRAINZ_ALBUM_DATA -> new DiscoveryTask.AlbumParameter("album1");
        };
        return new DiscoveryTask().setId("task1").setCreationDate(endDate.minusMinutes(15)).setUpdateDate(endDate)
                .setStatus(status).setType(type).setParameter(JsonConverter.toJson(parameter));
    }

    private DiscoveryJob startJob(DiscoveryType type, boolean cacheEnabled) throws ConcurrentLibraryJobException {
        return switch (type) {
            case FULL -> discoveryJobService.startFullJob(cacheEnabled);
            case ARTIST -> {
                when(libraryService.getArtistById("artistId")).thenReturn(Optional.of(new Artist().setId("artistId")));
                yield discoveryJobService.startArtistJob("artistId", cacheEnabled);
            }
            case ALBUM -> {
                when(libraryService.getAlbumById("albumId")).thenReturn(Optional.of(new Album().setId("albumId")));
                yield discoveryJobService.startAlbumJob("albumId", cacheEnabled);
            }
        };
    }

    private String startJobPath(DiscoveryType type) {
        return switch (type) {
            case FULL -> "/api/admin/library/discoveryJobs/full";
            case ARTIST -> "/api/admin/library/discoveryJobs/artist/artistId";
            case ALBUM -> "/api/admin/library/discoveryJobs/album/albumId";
        };
    }

    private DiscoveryJob completedJob() {
        return discoveryJob(DiscoveryType.FULL)
                .setId("job1")
                .setStatus(DiscoveryJob.Status.MODERATE);
    }

    private void mockProgress(String suffix, DiscoveryJobProgress progress) {
        if (suffix.isEmpty()) {
            when(discoveryJobService.getCurrentDiscoveryJobProgress()).thenReturn(Optional.of(progress));
        } else {
            when(discoveryJobService.getDiscoveryJobProgress("job1")).thenReturn(Optional.of(progress));
        }
    }
}
