package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.web.service.LlmEvaluationExportService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LlmEvaluationDownloadTest {

    private final LlmEvaluationExportService exportService = mock(LlmEvaluationExportService.class);
    private final LlmAdminController controller = new LlmAdminController(mock(LlmCacheService.class), exportService);

    @Test
    void shouldRejectOverlappingDownloadAndReleaseSlotAfterCompletion() throws IOException {
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
    void shouldLeavePartialDownloadUntouchedWhenExportFailsAfterCommit() throws IOException {
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

}
