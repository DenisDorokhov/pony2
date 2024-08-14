package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.log.repository.LogMessageRepository;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.LogMessageDto;
import net.dorokhov.pony2.web.dto.LogMessagePageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class LogAdminControllerTest extends InstallingIntegrationTest {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private ApiTemplate apiTemplate;

    @Autowired
    private LogMessageRepository logMessageRepository;
    
    @Autowired
    private LogService logService;

    private AuthenticationDto authentication;

    @BeforeEach
    void setUp() {
        authentication = apiTemplate.authenticateAdmin();
        logMessageRepository.deleteAll();
    }

    @Test
    public void shouldGetLog() {

        LogMessage logMessage = logService.info(logger, "someMessage").orElseThrow();

        ResponseEntity<LogMessagePageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(log -> checkLog(logMessage, log));
    }

    @Test
    public void shouldGetLogByDebugLevel() {

        LogMessage debug = logService.debug(logger, "debugMessage").orElseThrow();
        LogMessage info = logService.info(logger, "infoMessage").orElseThrow();
        LogMessage warn = logService.warn(logger, "warnMessage").orElseThrow();
        LogMessage error = logService.error(logger, "errorMessage").orElseThrow();

        ResponseEntity<LogMessagePageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log?minLevel=DEBUG", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(logs -> {
            assertThat(logs.getLogMessages()).hasSize(4);
            checkLogMessage(getByLevel(logs, LogMessage.Level.ERROR), error);
            checkLogMessage(getByLevel(logs, LogMessage.Level.WARN), warn);
            checkLogMessage(getByLevel(logs, LogMessage.Level.INFO), info);
            checkLogMessage(getByLevel(logs, LogMessage.Level.DEBUG), debug);
        });
    }

    private LogMessageDto getByLevel(LogMessagePageDto page, LogMessage.Level level) {
        return page.getLogMessages().stream()
                .filter(next -> next.getLevel() == level)
                .findFirst()
                .orElseThrow();
    }

    @Test
    public void shouldGetLogByInfoLevel() {

        logService.debug(logger, "debugMessage");
        LogMessage info = logService.info(logger, "infoMessage").orElseThrow();
        LogMessage warn = logService.warn(logger, "warnMessage").orElseThrow();
        LogMessage error = logService.error(logger, "errorMessage").orElseThrow();

        ResponseEntity<LogMessagePageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log?minLevel=INFO", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(logs -> {
            assertThat(logs.getLogMessages()).hasSize(3);
            checkLogMessage(getByLevel(logs, LogMessage.Level.ERROR), error);
            checkLogMessage(getByLevel(logs, LogMessage.Level.WARN), warn);
            checkLogMessage(getByLevel(logs, LogMessage.Level.INFO), info);
        });
    }

    @Test
    public void shouldGetLogByWarnLevel() {

        logService.debug(logger, "debugMessage");
        logService.info(logger, "infoMessage");
        LogMessage warn = logService.warn(logger, "warnMessage").orElseThrow();
        LogMessage error = logService.error(logger, "errorMessage").orElseThrow();

        ResponseEntity<LogMessagePageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log?minLevel=WARN", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(logs -> {
            assertThat(logs.getLogMessages()).hasSize(2);
            checkLogMessage(getByLevel(logs, LogMessage.Level.ERROR), error);
            checkLogMessage(getByLevel(logs, LogMessage.Level.WARN), warn);
        });
    }

    @Test
    public void shouldGetLogByErrorLevel() {

        logService.debug(logger, "debugMessage");
        logService.info(logger, "infoMessage");
        logService.warn(logger, "warnMessage");
        LogMessage error = logService.error(logger, "errorMessage").orElseThrow();

        ResponseEntity<LogMessagePageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log?minLevel=ERROR", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(logs -> checkLog(error, logs));
    }

    @Test
    public void shouldGetLogByMinAndMaxDate() {
        
        LogMessage logMessage = logService.info(logger, "someMessage").orElseThrow();

        ResponseEntity<LogMessagePageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log?minDate=1986-05-04T00:00:00.000&maxDate=3000-05-04T00:00:00.000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(log -> checkLog(logMessage, log));
        
        response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log?minDate=3000-05-04T00:00:00.000&maxDate=3000-05-04T00:00:00.000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(this::checkEmptyLog);
    }

    @Test
    public void shouldGetLogByPageIndex() {

        logService.info(logger, "someMessage");

        ResponseEntity<LogMessagePageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log?pageIndex=1", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(log -> {
            assertThat(log.getPageIndex()).isEqualTo(1);
            assertThat(log.getPageSize()).isGreaterThan(0);
            assertThat(log.getTotalPages()).isEqualTo(1);
            assertThat(log.getLogMessages()).isEmpty();
        });
    }

    private void checkLog(LogMessage logMessage, LogMessagePageDto log) {
        assertThat(log.getPageIndex()).isEqualTo(0);
        assertThat(log.getPageSize()).isGreaterThan(0);
        assertThat(log.getTotalPages()).isEqualTo(1);
        assertThat(log.getLogMessages()).hasSize(1);
        assertThat(log.getLogMessages()).first().satisfies(logMessageDto ->
                checkLogMessage(logMessageDto, logMessage));
    }
    
    private void checkLogMessage(LogMessageDto dto, LogMessage logMessage) {
        assertThat(dto.getLevel()).isEqualTo(logMessage.getLevel());
        assertThat(dto.getId()).isEqualTo(logMessage.getId());
        assertThat(dto.getDate()).isEqualTo(logMessage.getDate());
        assertThat(dto.getPattern()).isEqualTo(logMessage.getPattern());
        assertThat(dto.getArguments()).isEqualTo(logMessage.getArguments());
        assertThat(dto.getText()).isEqualTo(logMessage.getText());
    }

    private void checkEmptyLog(LogMessagePageDto log) {
        assertThat(log.getPageIndex()).isEqualTo(0);
        assertThat(log.getPageSize()).isGreaterThan(0);
        assertThat(log.getTotalPages()).isEqualTo(0);
        assertThat(log.getLogMessages()).isEmpty();
    }
}
