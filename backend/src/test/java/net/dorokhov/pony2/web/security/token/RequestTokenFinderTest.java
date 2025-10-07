package net.dorokhov.pony2.web.security.token;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestTokenFinderTest {

    private final RequestTokenFinder requestTokenFinder = new RequestTokenFinder();

    @Test
    public void shouldFetchAccessTokenFromAuthorizationHeader() {

        MockHttpServletRequest request = mockHttpServletRequest();
        request.addHeader("Authorization", "Bearer someToken");

        assertThat(requestTokenFinder.findAccessToken(request)).isEqualTo("someToken");
    }

    @Test
    public void shouldFetchStaticTokenFromCookie() {

        MockHttpServletRequest request = mockHttpServletRequest();
        request.setCookies(new Cookie(RequestTokenFinder.TOKEN_COOKIE_NAME, "someToken"));

        assertThat(requestTokenFinder.findStaticToken(request)).isEqualTo("someToken");
    }

    @Test
    public void shouldFetchOpenSubsonicApiKeyFromParameters() {

        MockHttpServletRequest request = mockHttpServletRequest();
        request.addParameter(RequestTokenFinder.TOKEN_PARAM_NAME, "someApiKey");

        assertThat(requestTokenFinder.findOpenSubsonicApiKey(request)).isEqualTo("someApiKey");
    }

    @Test
    public void shouldSupportNoAccessTokenInAuthorizationHeader() {

        MockHttpServletRequest request = mockHttpServletRequest();

        assertThat(requestTokenFinder.findAccessToken(request)).isNull();
    }

    @Test
    public void shouldSupportNoBearerAuthorizationHeader() {

        MockHttpServletRequest request = mockHttpServletRequest();
        request.addHeader("Authorization", "Basic someToken");

        assertThat(requestTokenFinder.findAccessToken(request)).isNull();
    }

    @Test
    public void shouldSupportNoStaticTokenInCookies() {

        MockHttpServletRequest request = mockHttpServletRequest("GET", "/api/file/someFile");

        assertThat(requestTokenFinder.findAccessToken(request)).isNull();
    }

    @Test
    public void shouldSupportNoOpenSubsonicApiKeyInParameters() {

        MockHttpServletRequest request = mockHttpServletRequest("GET", "/opensubsonic/rest/ping.view");

        assertThat(requestTokenFinder.findOpenSubsonicApiKey(request)).isNull();
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