package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.api.llm.service.DiscoveryCacheService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.web.service.DiscoveryFacade;
import net.dorokhov.pony2.web.service.LibraryFacade;
import net.dorokhov.pony2.web.service.LlmEvaluationExportService;
import net.dorokhov.pony2.web.service.ScanFacade;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LlmEvaluationDownloadTest {

    private final LlmEvaluationExportService exportService = mock(LlmEvaluationExportService.class);
    private final LibraryJobSynchronizer jobSynchronizer = new LibraryJobSynchronizer(new ShutdownService());
    private final LibraryAdminController controller = new LibraryAdminController(mock(ScanFacade.class),
            mock(DiscoveryFacade.class), mock(LibraryFacade.class), mock(DiscoveryCacheService.class),
            exportService, jobSynchronizer);

    @Test
    void shouldRejectOverlappingDownloadAndReleaseSlotAfterCompletion() throws Exception {
        MockHttpServletResponse overlapping = new MockHttpServletResponse();
        doAnswer(invocation -> {
            controller.downloadEvaluation(overlapping);
            return null;
        }).doNothing().when(exportService).write(any(), any());

        controller.downloadEvaluation(new MockHttpServletResponse());
        assertThat(overlapping.getStatus()).isEqualTo(429);

        MockHttpServletResponse next = new MockHttpServletResponse();
        controller.downloadEvaluation(next);
        assertThat(next.getStatus()).isEqualTo(200);
        verify(exportService, times(2)).write(any(), any());
    }

    @Test
    void shouldLeavePartialDownloadUntouchedWhenExportFailsAfterCommit() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(0);
            output.write("{\"exportedAt\":\"2026-10-09T23:45\",".getBytes(StandardCharsets.UTF_8));
            response.flushBuffer();
            throw new IllegalStateException("Database unavailable");
        }).doNothing().when(exportService).write(any(), any());

        controller.downloadEvaluation(response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("{\"exportedAt\":\"2026-10-09T23:45\",");
        MockHttpServletResponse next = new MockHttpServletResponse();
        controller.downloadEvaluation(next);
        assertThat(next.getStatus()).isEqualTo(200);
        verify(exportService, times(2)).write(any(), any());
    }

    @Test
    void shouldHoldLibraryLockUntilExportFinishes() throws Exception {
        doAnswer(invocation -> {
            assertThatThrownBy(jobSynchronizer::registerDiscoveryJob).isInstanceOf(ConcurrentLibraryJobException.class);
            assertThatThrownBy(() -> jobSynchronizer.registerScanJob(Duration.ZERO))
                    .isInstanceOf(ConcurrentLibraryJobException.class);
            assertThatThrownBy(jobSynchronizer::registerMaintenanceJob).isInstanceOf(ConcurrentLibraryJobException.class);
            return null;
        }).when(exportService).write(any(), any());

        controller.downloadEvaluation(new MockHttpServletResponse());

        try (LibraryJobSynchronizer.LibraryJobRegistration ignored = jobSynchronizer.registerDiscoveryJob()) {
            assertThat(jobSynchronizer.isDiscoveryCancelled()).isFalse();
        }
    }

}
