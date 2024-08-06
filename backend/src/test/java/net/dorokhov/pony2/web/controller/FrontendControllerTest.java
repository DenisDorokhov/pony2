package net.dorokhov.pony2.web.controller;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.FrontendLogCommandDto;
import org.junit.jupiter.api.Test;
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
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto()
                .setLevel(FrontendLogCommandDto.Level.TRACE)
                .setMessage("test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAcceptDebugLog() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto()
                .setLevel(FrontendLogCommandDto.Level.DEBUG)
                .setMessage("test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAcceptInfoLog() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto()
                .setLevel(FrontendLogCommandDto.Level.INFO)
                .setMessage("test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAcceptWarnLog() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto()
                .setLevel(FrontendLogCommandDto.Level.WARN)
                .setMessage("test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAcceptErrorLog() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        FrontendLogCommandDto logCommand = new FrontendLogCommandDto()
                .setLevel(FrontendLogCommandDto.Level.ERROR)
                .setMessage("test");

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/frontend/log", HttpMethod.POST,
                apiTemplate.createHeaderRequest(ImmutableList.of(logCommand), authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }
}
