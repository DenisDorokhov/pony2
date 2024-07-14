package net.dorokhov.pony3.web.controller;

import net.dorokhov.pony3.ApiTemplate;
import net.dorokhov.pony3.InstallingIntegrationTest;
import net.dorokhov.pony3.api.log.domain.LogMessage;
import net.dorokhov.pony3.api.log.service.LogService;
import net.dorokhov.pony3.web.dto.AuthenticationDto;
import net.dorokhov.pony3.web.dto.LogMessageDto;
import net.dorokhov.pony3.web.dto.LogMessagePageDto;
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
    private LogService logService;

    @Test
    public void shouldGetLog() {

        LogMessage logMessage = logService.info(logger, "someMessage");
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<LogMessagePageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(log -> checkLog(logMessage, log));
    }

    @Test
    public void shouldGetLogByMinLevel() {

        logService.info(logger, "someMessage");
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<LogMessagePageDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/log?minLevel=WARN", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), LogMessagePageDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(this::checkEmptyLog);
    }

    @Test
    public void shouldGetLogByMinAndMaxDate() {
        
        LogMessage logMessage = logService.info(logger, "someMessage");
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        
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
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

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
    
    @SuppressWarnings("Duplicates")
    private void checkLogMessage(LogMessageDto dto, LogMessage logMessage) {
        assertThat(dto.getId()).isEqualTo(logMessage.getId());
        assertThat(dto.getDate()).isEqualTo(logMessage.getDate());
        assertThat(dto.getLevel()).isEqualTo(logMessage.getLevel());
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
