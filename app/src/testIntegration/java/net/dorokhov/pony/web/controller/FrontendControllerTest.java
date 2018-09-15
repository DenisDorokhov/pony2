package net.dorokhov.pony.web.controller;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.ApiTemplate;
import net.dorokhov.pony.InstallingIntegrationTest;
import net.dorokhov.pony.web.domain.AuthenticationDto;
import net.dorokhov.pony.web.domain.FrontendLogCommandDto;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class FrontendControllerTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;

    @Test
    public void shouldAcceptTraceLog() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto(FrontendLogCommandDto.Level.TRACE, "test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAcceptDebugLog() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto(FrontendLogCommandDto.Level.DEBUG, "test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAcceptInfoLog() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto(FrontendLogCommandDto.Level.INFO, "test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAcceptWarnLog() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto(FrontendLogCommandDto.Level.WARN, "test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAcceptErrorLog() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto(FrontendLogCommandDto.Level.ERROR, "test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }
}
