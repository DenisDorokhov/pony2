package net.dorokhov.pony.web.security.token;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestTokenFinderTest {
    
    private RequestTokenFinder requestTokenFinder = new RequestTokenFinder();

    @Test
    public void shouldFetchTokenFromHeaders() throws Exception {
        MockHttpServletRequest request = mockHttpServletRequest();
        request.addHeader("Authorization", "Bearer someToken");
        assertThat(requestTokenFinder.findToken(request)).isEqualTo("someToken");
    }

    @Test
    public void shouldFetchTokenFromCookiesWhenRequestIsFileAndMethodIsGET() throws Exception {
        MockHttpServletRequest request = mockHttpServletRequest("GET", "/api/file/someFile");
        request.setCookies(new Cookie(RequestTokenFinder.TOKEN_COOKIE_NAME, "someToken"));
        assertThat(requestTokenFinder.findToken(request)).isEqualTo("someToken");
    }

    @Test
    public void shouldNotFetchTokenFromCookiesIfRequestIsNotFile() throws Exception {
        MockHttpServletRequest request = mockHttpServletRequest("GET", "/api/someApi");
        request.setCookies(new Cookie(RequestTokenFinder.TOKEN_COOKIE_NAME, "someToken"));
        assertThat(requestTokenFinder.findToken(request)).isNull();
    }

    @Test
    public void shouldNotFetchTokenFromCookiesIfRequestIsFileAndRequestMethodIsNotGET() throws Exception {
        MockHttpServletRequest request = mockHttpServletRequest("POST", "/api/file/someFile");
        request.setCookies(new Cookie(RequestTokenFinder.TOKEN_COOKIE_NAME, "someToken"));
        assertThat(requestTokenFinder.findToken(request)).isNull();
    }

    @Test
    public void shouldSupportNoTokenInHeaders() throws Exception {
        MockHttpServletRequest request = mockHttpServletRequest();
        assertThat(requestTokenFinder.findToken(request)).isNull();
    }

    @Test
    public void shouldSupportNoBearerAuthorizationHeaders() throws Exception {
        MockHttpServletRequest request = mockHttpServletRequest();
        request.addHeader("Authorization", "Basic someToken");
        assertThat(requestTokenFinder.findToken(request)).isNull();
    }

    @Test
    public void shouldSupportNoTokenInCookiesIfRequestIsFileAndMethodIsGET() throws Exception {
        MockHttpServletRequest request = mockHttpServletRequest("GET", "/api/file/someFile");
        assertThat(requestTokenFinder.findToken(request)).isNull();
    }
    
    private MockHttpServletRequest mockHttpServletRequest() {
        return mockHttpServletRequest("GET", "/api/someApi");
    }
    
    private MockHttpServletRequest mockHttpServletRequest(String method, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, servletPath);
        request.setServletPath(servletPath);
        return request;
    }
}