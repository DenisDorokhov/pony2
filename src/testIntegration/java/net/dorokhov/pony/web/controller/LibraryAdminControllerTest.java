package net.dorokhov.pony.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.ApiTemplate;
import net.dorokhov.pony.InstallingIntegrationTest;
import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.fixture.BlockingScanJobServiceObserver;
import net.dorokhov.pony.fixture.ScanTestPlan;
import net.dorokhov.pony.fixture.ScanTestPlanExecutor;
import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.domain.ScanJob.Status;
import net.dorokhov.pony.library.domain.ScanJobProgress;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanType;
import net.dorokhov.pony.library.service.ScanJobService;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.log.domain.LogMessage;
import net.dorokhov.pony.web.domain.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Repeat;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class LibraryAdminControllerTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;

    @Autowired
    private ScanJobService scanJobService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ScanTestPlanExecutor scanTestPlanExecutor;
    
    private final BlockingScanJobServiceObserver blockingObserver = new BlockingScanJobServiceObserver();

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        scanJobService.removeObserver(blockingObserver);
        scanTestPlanExecutor.clean();
    }

    @Test
    public void shouldPerformFullScan() throws Exception {

        ScanTestPlan scanTestPlan = objectMapper.readValue(new ClassPathResource("scan-test-01-init.json").getFile(), ScanTestPlan.class);
        ScanTestPlanExecutor.Context context = scanTestPlanExecutor.prepare(scanTestPlan);
        configService.saveLibraryFolders(ImmutableList.of(context.getRootFolder()));

        scanJobService.addObserver(blockingObserver);

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ScanJobDto> scanJobResponse = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs/full", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getToken()), ScanJobDto.class);

        assertThat(scanJobResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        ScanJobDto scanJob = scanJobResponse.getBody();
        assertThat(scanJob.getId()).isNotNull();
        assertThat(scanJob.getCreationDate()).isNotNull();
        assertThat(scanJob.getUpdateDate()).isNull();
        assertThat(scanJob.getScanType()).isSameAs(ScanType.FULL);
        assertThat(scanJob.getStatus()).isSameAs(Status.STARTING);
        assertThat(scanJob.getLogMessage()).isNotNull();
        assertThat(scanJob.getScanResult()).isNull();

        await().until(() -> {
            ScanJobProgress scanJobProgress = scanJobService.getScanJobProgress(scanJob.getId());
            return scanJobProgress != null && scanJobProgress.getScanProgress() != null;
        });

        ResponseEntity<ScanJobProgressDto> scanJobProgressResponse = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobProgress", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ScanJobProgressDto.class);

        assertThat(scanJobProgressResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(scanJobProgressResponse.getBody()).satisfies(scanJobProgress -> {
            assertThat(scanJobProgress.getScanJob()).isNotNull();
            assertThat(scanJobProgress.getScanProgress()).isNotNull();
        });

        scanJobProgressResponse = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobProgress/{scanJobId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ScanJobProgressDto.class, scanJob.getId());

        assertThat(scanJobProgressResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(scanJobProgressResponse.getBody()).satisfies(scanJobProgress -> {
            assertThat(scanJobProgress.getScanJob()).isNotNull();
            assertThat(scanJobProgress.getScanProgress()).isNotNull();
        });
        
        blockingObserver.unlock();

        await().atMost(30, SECONDS).until(() -> {
            Status status = scanJobService.getById(scanJob.getId()).getStatus();
            return status == Status.COMPLETE || status == Status.FAILED;
        });
        scanTestPlanExecutor.verify(scanJob.getId(), context);

        runAndVerifyDeletionDuringScan();
        runAndVerifyModificationDuringScan();
    }

    @Test
    @Repeat(10)
    public void shouldPerformFullScanRepeatedly() throws Exception {
        shouldPerformFullScan();
    }

    @Test
    public void shouldFailFullScanIfItIsAlreadyRunning() throws Exception {

        scanJobService.addObserver(blockingObserver);
        ScanJob scanJob = scanJobService.startScanJob();

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs/full", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getToken()), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> 
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.CONCURRENT_SCAN));
        
        blockingObserver.unlock();

        await().until(() -> {
            Status status = scanJobService.getById(scanJob.getId()).getStatus();
            return status == Status.COMPLETE || status == Status.FAILED;
        });
    }

    @Test
    public void shouldGetAllScanJobs() throws Exception {
        Long scanJob1 = createAndFinishScanJob();
        Long scanJob2 = createAndFinishScanJob();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ScanJobPageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ScanJobPageDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(scanJobPage -> {
            assertThat(scanJobPage.getPageIndex()).isEqualTo(0);
            assertThat(scanJobPage.getPageSize()).isGreaterThan(0);
            assertThat(scanJobPage.getTotalPages()).isEqualTo(1);
            assertThat(scanJobPage.getScanJobs()).satisfies(scanJobs -> {
                //noinspection unchecked
                assertThat(scanJobs).hasSize(2);
                checkScanJobDto(scanJobs.get(0), scanJob2);
                checkScanJobDto(scanJobs.get(1), scanJob1);
            });
        });
    }

    @Test
    public void shouldGetScanJobById() throws Exception {
        Long scanJobId = createAndFinishScanJob();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ScanJobDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs/{scanJobId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ScanJobDto.class, scanJobId);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(dto -> checkScanJobDto(dto, scanJobId));
    }

    @Test
    public void shouldFailGettingScanJobIfScanJobNotFound() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs/1000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("ScanJob");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldFailGettingCurrentScanJobProgressWhenThereIsNoScanJobRunning() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobProgress", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("ScanJobProgress");
        });
    }

    @Test
    public void shouldFailGettingScanJobProgressWhenScanJobIsNotFound() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobProgress/1000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("ScanJob");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    private Long createAndFinishScanJob() throws ConcurrentScanException {
        ScanJob scanJob = scanJobService.startScanJob();
        await().until(() -> scanJobService.getById(scanJob.getId()).getStatus() == Status.COMPLETE);
        return scanJob.getId();
    }

    private void checkScanJobDto(ScanJobDto dto, Long scanJobId) {
        getTransactionTemplate().execute(status -> {
            ScanJob scanJob = scanJobService.getById(scanJobId);
            assertThat(dto.getId()).isEqualTo(scanJob.getId());
            assertThat(dto.getCreationDate()).isEqualTo(scanJob.getCreationDate());
            assertThat(dto.getUpdateDate()).isEqualTo(scanJob.getUpdateDate());
            assertThat(dto.getScanType()).isEqualTo(scanJob.getScanType());
            assertThat(dto.getStatus()).isEqualTo(scanJob.getStatus());
            checkLogMessageDto(dto.getLogMessage(), scanJob.getLogMessage());
            checkScanResultDto(dto.getScanResult(), scanJob.getScanResult());
            return null;
        });
    }

    @SuppressWarnings("Duplicates")
    private void checkLogMessageDto(LogMessageDto dto, LogMessage logMessage) {
        assertThat(dto.getId()).isEqualTo(logMessage.getId());
        assertThat(dto.getDate()).isEqualTo(logMessage.getDate());
        assertThat(dto.getLevel()).isEqualTo(logMessage.getLevel());
        assertThat(dto.getPattern()).isEqualTo(logMessage.getPattern());
        assertThat(dto.getArguments()).isEqualTo(logMessage.getArguments());
        assertThat(dto.getText()).isEqualTo(logMessage.getText());
    }

    private void checkScanResultDto(ScanResultDto dto, ScanResult scanResult) {
        assertThat(dto.getId()).isEqualTo(scanResult.getId());
        assertThat(dto.getDate()).isEqualTo(scanResult.getDate());
        assertThat(dto.getScanType()).isEqualTo(scanResult.getScanType());
        assertThat(dto.getTargetPaths()).isEqualTo(scanResult.getTargetPaths());
        assertThat(dto.getFailedPaths()).isEqualTo(scanResult.getFailedPaths());
        assertThat(dto.getProcessedAudioFileCount()).isEqualTo(scanResult.getProcessedAudioFileCount());
        assertThat(dto.getDuration()).isEqualTo(scanResult.getDuration());
        assertThat(dto.getSongSize()).isEqualTo(scanResult.getSongSize());
        assertThat(dto.getArtworkSize()).isEqualTo(scanResult.getArtworkSize());
        assertThat(dto.getGenreCount()).isEqualTo(scanResult.getGenreCount());
        assertThat(dto.getArtistCount()).isEqualTo(scanResult.getArtistCount());
        assertThat(dto.getAlbumCount()).isEqualTo(scanResult.getAlbumCount());
        assertThat(dto.getSongCount()).isEqualTo(scanResult.getSongCount());
        assertThat(dto.getArtworkCount()).isEqualTo(scanResult.getArtworkCount());
        assertThat(dto.getCreatedArtistCount()).isEqualTo(scanResult.getCreatedArtistCount());
        assertThat(dto.getUpdatedArtistCount()).isEqualTo(scanResult.getUpdatedArtistCount());
        assertThat(dto.getDeletedArtistCount()).isEqualTo(scanResult.getDeletedArtistCount());
        assertThat(dto.getCreatedAlbumCount()).isEqualTo(scanResult.getCreatedAlbumCount());
        assertThat(dto.getUpdatedAlbumCount()).isEqualTo(scanResult.getUpdatedAlbumCount());
        assertThat(dto.getDeletedAlbumCount()).isEqualTo(scanResult.getDeletedAlbumCount());
        assertThat(dto.getCreatedSongCount()).isEqualTo(scanResult.getCreatedSongCount());
        assertThat(dto.getUpdatedSongCount()).isEqualTo(scanResult.getUpdatedSongCount());
        assertThat(dto.getDeletedSongCount()).isEqualTo(scanResult.getDeletedSongCount());
        assertThat(dto.getCreatedGenreCount()).isEqualTo(scanResult.getCreatedGenreCount());
        assertThat(dto.getUpdatedGenreCount()).isEqualTo(scanResult.getUpdatedGenreCount());
        assertThat(dto.getDeletedGenreCount()).isEqualTo(scanResult.getDeletedGenreCount());
        assertThat(dto.getCreatedArtworkCount()).isEqualTo(scanResult.getCreatedArtworkCount());
        assertThat(dto.getDeletedArtworkCount()).isEqualTo(scanResult.getDeletedArtworkCount());
    }

    private void runAndVerifyDeletionDuringScan() throws Exception {

        ScanTestPlan scanTestPlan = objectMapper.readValue(new ClassPathResource("scan-test-02-delete.json").getFile(), ScanTestPlan.class);
        ScanTestPlanExecutor.Context context = scanTestPlanExecutor.prepare(scanTestPlan);
        ScanJob scanJob = scanJobService.startScanJob();

        await().atMost(30, SECONDS).until(() -> {
            Status status = scanJobService.getById(scanJob.getId()).getStatus();
            return status == Status.COMPLETE || status == Status.FAILED;
        });
        scanTestPlanExecutor.verify(scanJob.getId(), context);
    }

    private void runAndVerifyModificationDuringScan() throws Exception {

        ScanTestPlan scanTestPlan = objectMapper.readValue(new ClassPathResource("scan-test-03-modify.json").getFile(), ScanTestPlan.class);
        ScanTestPlanExecutor.Context context = scanTestPlanExecutor.prepare(scanTestPlan);
        ScanJob scanJob = scanJobService.startScanJob();

        await().atMost(30, SECONDS).until(() -> {
            Status status = scanJobService.getById(scanJob.getId()).getStatus();
            return status == Status.COMPLETE || status == Status.FAILED;
        });
        scanTestPlanExecutor.verify(scanJob.getId(), context);
    }
}
