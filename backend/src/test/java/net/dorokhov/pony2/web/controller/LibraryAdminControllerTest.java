package net.dorokhov.pony2.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.*;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.library.domain.ScanJob;
import net.dorokhov.pony2.api.library.domain.ScanJob.Status;
import net.dorokhov.pony2.api.library.domain.ScanJobProgress;
import net.dorokhov.pony2.api.library.domain.ScanResult;
import net.dorokhov.pony2.api.library.domain.ScanType;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.web.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Repeat;

import javax.annotation.Nullable;
import java.io.IOException;

import static java.util.Objects.requireNonNull;
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

    @AfterEach
    public void tearDown() {
        scanJobService.removeObserver(blockingObserver);
        scanTestPlanExecutor.clean();
    }

    @Test
    public void shouldPerformFullScan() throws IOException {
        runAndVerifyInitialScan();
    }

    @Test
    @Repeat(10)
    public void shouldPerformFullScanRepeatedly() throws IOException {
        shouldPerformFullScan();
    }

    @Test
    public void shouldPerformFullScanFlow() throws IOException, InterruptedException, ConcurrentScanException {
        
        runAndVerifyInitialScan();

        Thread.sleep(1000); // Make sure files are written with modification date in the future.
        runAndVerifyDeletionScan();

        Thread.sleep(1000); // Make sure files are written with modification date in the future.
        runAndVerifyModificationScan();
    }

    @Test
    public void shouldFailFullScanIfItIsAlreadyRunning() throws ConcurrentScanException {

        scanJobService.addObserver(blockingObserver);
        ScanJob scanJob = scanJobService.startScanJob();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> 
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.CONCURRENT_SCAN));
        
        blockingObserver.unlock();

        await().until(() -> {
            Status status = scanJobService.getById(scanJob.getId()).orElseThrow().getStatus();
            return status == Status.COMPLETE || status == Status.FAILED;
        });
    }

    @Test
    public void shouldGetAllScanJobs() throws ConcurrentScanException {

        String scanJob1 = createAndFinishScanJob();
        String scanJob2 = createAndFinishScanJob();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ScanJobPageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ScanJobPageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(scanJobPage -> {
            assertThat(scanJobPage.getPageIndex()).isEqualTo(0);
            assertThat(scanJobPage.getPageSize()).isGreaterThan(0);
            assertThat(scanJobPage.getTotalPages()).isEqualTo(1);
            assertThat(scanJobPage.getScanJobs()).satisfies(scanJobs -> {
                assertThat(scanJobs).hasSize(2);
                checkScanJobDto(scanJobs.get(0), scanJob2);
                checkScanJobDto(scanJobs.get(1), scanJob1);
            });
        });
    }

    @Test
    public void shouldGetScanJobById() throws ConcurrentScanException {

        String scanJobId = createAndFinishScanJob();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ScanJobDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs/{scanJobId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ScanJobDto.class, scanJobId);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(dto -> checkScanJobDto(dto, scanJobId));
    }

    @Test
    public void shouldFailGettingScanJobIfScanJobNotFound() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs/1000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("ScanJob");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldFailGettingCurrentScanJobProgressWhenThereIsNoScanJobRunning() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<OptionalResponseDto<ScanJobProgressDto>> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobProgress", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isFalse();
            assertThat(optionalResponse.getValue()).isNull();
        });
    }

    @Test
    public void shouldFailGettingScanJobProgressWhenScanJobIsNotFound() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<OptionalResponseDto<ScanJobProgressDto>> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobProgress/1000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isFalse();
            assertThat(optionalResponse.getValue()).isNull();
        });
    }
    
    private void runAndVerifyInitialScan() throws IOException {

        ScanTestPlan scanTestPlan = objectMapper.readValue(new ClassPathResource("scan-test-01-init.json").getFile(), ScanTestPlan.class);
        ScanTestPlanExecutor.Context context = scanTestPlanExecutor.prepare(scanTestPlan);
        configService.saveLibraryFolders(ImmutableList.of(context.getRootFolder()));
        scanJobService.addObserver(blockingObserver);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ScanJobDto> scanJobResponse = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobs", HttpMethod.POST,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ScanJobDto.class);

        assertThat(scanJobResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        ScanJobDto scanJob = requireNonNull(scanJobResponse.getBody());
        assertThat(scanJob.getId()).isNotNull();
        assertThat(scanJob.getCreationDate()).isNotNull();
        assertThat(scanJob.getUpdateDate()).isNull();
        assertThat(scanJob.getScanType()).isSameAs(ScanType.FULL);
        assertThat(scanJob.getStatus()).isSameAs(Status.STARTING);
        assertThat(scanJob.getLogMessage()).isNotNull();
        assertThat(scanJob.getScanResult()).isNull();

        await().until(() -> {
            ScanJobProgress scanJobProgress = scanJobService.getScanJobProgress(scanJob.getId()).orElse(null);
            return scanJobProgress != null && scanJobProgress.getScanProgress() != null;
        });

        ResponseEntity<OptionalResponseDto<ScanJobProgressDto>> optionalScanJobProgressResponse = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobProgress", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()),
                new ParameterizedTypeReference<>() {});

        assertThat(optionalScanJobProgressResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(optionalScanJobProgressResponse.getBody()).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isTrue();
            assertThat(optionalResponse.getValue()).satisfies(scanJobProgress -> {
                assertThat(scanJobProgress.getScanJob()).isNotNull();
                assertThat(scanJobProgress.getScanProgress()).isNotNull();
            });
        });

        ResponseEntity<OptionalResponseDto<ScanJobProgressDto>> scanJobProgressResponse = apiTemplate.getRestTemplate().exchange(
                "/api/admin/library/scanJobProgress/{scanJobId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()),
                new ParameterizedTypeReference<>() {},
                scanJob.getId());

        assertThat(scanJobProgressResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(scanJobProgressResponse.getBody()).satisfies(optionalResponse -> {
            assertThat(optionalResponse.isPresent()).isTrue();
            assertThat(optionalResponse.getValue().getScanJob()).isNotNull();
            assertThat(optionalResponse.getValue().getScanProgress()).isNotNull();
        });

        blockingObserver.unlock();

        await().atMost(30, SECONDS).until(() -> {
            Status status = scanJobService.getById(scanJob.getId()).orElseThrow().getStatus();
            return status == Status.COMPLETE || status == Status.FAILED;
        });
        scanTestPlanExecutor.verify(scanJob.getId(), context);
    }

    private String createAndFinishScanJob() throws ConcurrentScanException {
        ScanJob scanJob = scanJobService.startScanJob();
        await().until(() -> scanJobService.getById(scanJob.getId()).orElseThrow().getStatus() == Status.COMPLETE);
        return scanJob.getId();
    }

    private void checkScanJobDto(ScanJobDto dto, String scanJobId) {
        getTransactionTemplate().execute(transactionStatus -> {
            ScanJob scanJob = scanJobService.getById(scanJobId).orElseThrow();
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
    private void checkLogMessageDto(@Nullable LogMessageDto dto, @Nullable LogMessage logMessage) {
        assertThat(logMessage).isNotNull();
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(logMessage.getId());
        assertThat(dto.getDate()).isEqualTo(logMessage.getDate());
        assertThat(dto.getLevel()).isEqualTo(logMessage.getLevel());
        assertThat(dto.getPattern()).isEqualTo(logMessage.getPattern());
        assertThat(dto.getArguments()).isEqualTo(logMessage.getArguments());
        assertThat(dto.getText()).isEqualTo(logMessage.getText());
    }

    private void checkScanResultDto(@Nullable ScanResultDto dto, @Nullable ScanResult scanResult) {
        assertThat(scanResult).isNotNull();
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(scanResult.getId());
        assertThat(dto.getDate()).isEqualTo(scanResult.getDate());
        assertThat(dto.getScanType()).isEqualTo(scanResult.getScanType());
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

    private void runAndVerifyDeletionScan() throws IOException, ConcurrentScanException {

        ScanTestPlan scanTestPlan = objectMapper.readValue(new ClassPathResource("scan-test-02-delete.json").getFile(), ScanTestPlan.class);
        ScanTestPlanExecutor.Context context = scanTestPlanExecutor.prepare(scanTestPlan);
        ScanJob scanJob = scanJobService.startScanJob();

        await().atMost(30, SECONDS).until(() -> {
            Status status = scanJobService.getById(scanJob.getId()).orElseThrow().getStatus();
            return status == Status.COMPLETE || status == Status.FAILED;
        });
        scanTestPlanExecutor.verify(scanJob.getId(), context);
    }

    private void runAndVerifyModificationScan() throws IOException, ConcurrentScanException {

        ScanTestPlan scanTestPlan = objectMapper.readValue(new ClassPathResource("scan-test-03-modify.json").getFile(), ScanTestPlan.class);
        ScanTestPlanExecutor.Context context = scanTestPlanExecutor.prepare(scanTestPlan);
        ScanJob scanJob = scanJobService.startScanJob();

        await().atMost(30, SECONDS).until(() -> {
            Status status = scanJobService.getById(scanJob.getId()).orElseThrow().getStatus();
            return status == Status.COMPLETE || status == Status.FAILED;
        });
        scanTestPlanExecutor.verify(scanJob.getId(), context);
    }
}
