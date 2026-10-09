package net.dorokhov.pony2.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.dorokhov.pony2.web.security.handler.AuthenticationFailureHandlerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Logbook;

import java.io.IOException;

import static org.mockito.Mockito.*;

class WebConfigTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(Logbook.class);
    private final HttpRequest request = mock(HttpRequest.class, RETURNS_SELF);
    private final HttpResponse response = mock(HttpResponse.class, RETURNS_SELF);
    private final Logbook logbook = new WebConfig(
            mock(SecurityContextRepository.class),
            mock(AuthenticationEntryPoint.class),
            mock(AccessDeniedHandler.class),
            mock(AuthenticationSuccessHandler.class),
            mock(AuthenticationFailureHandlerImpl.class),
            mock(LogoutSuccessHandler.class)
    ).logbook();
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.TRACE);
        when(request.getHeaders()).thenReturn(HttpHeaders.empty());
        when(request.getQuery()).thenReturn("");
        when(response.getHeaders()).thenReturn(HttpHeaders.empty());
        when(response.getStatus()).thenReturn(200);
    }

    @AfterEach
    void tearDown() {
        logger.setLevel(previousLevel);
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/file/audio/song-id",
            "GET, /api/file/artwork/large/artwork-id",
            "GET, /api/file/artwork/small/artwork-id",
            "GET, /api/file/export/song/song-id",
            "GET, /api/file/export/album/album-id",
            "GET, /api/admin/llm/evaluation",
            "GET, /api/admin/playlists/backup",
            "POST, /api/admin/playlists/restore",
            "GET, /api/admin/history/backup",
            "POST, /api/admin/history/restore",
            "GET, /opensubsonic/rest/stream.view",
            "POST, /opensubsonic/rest/stream.view",
            "GET, /opensubsonic/rest/download.view",
            "POST, /opensubsonic/rest/download.view",
            "GET, /opensubsonic/rest/getCoverArt.view",
            "POST, /opensubsonic/rest/getCoverArt.view"
    })
    void shouldNotBufferFileOrBackupBodiesEvenWhenTraceIsEnabled(String method, String path) throws IOException {
        when(request.getMethod()).thenReturn(method);
        when(request.getPath()).thenReturn(path);

        logbook.process(request).write().process(response).write();

        verify(request, never()).withBody();
        verify(response, never()).withBody();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/config", "/api/library/artists", "/opensubsonic/rest/getArtists.view"})
    void shouldKeepLoggingEnabledForOtherEndpoints(String path) throws IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getPath()).thenReturn(path);

        logbook.process(request).write().process(response).write();

        verify(request).withBody();
        verify(response).withBody();
    }
}
