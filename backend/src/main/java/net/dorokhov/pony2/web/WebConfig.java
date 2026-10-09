package net.dorokhov.pony2.web;

import net.dorokhov.pony2.web.security.WebAuthority;
import net.dorokhov.pony2.web.security.handler.AuthenticationFailureHandlerImpl;
import net.dorokhov.pony2.web.service.OpenSubsonicResponseService;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.core.*;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.security.authorization.AuthorizationManagers.allOf;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;
import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;
import static org.zalando.logbook.core.Conditions.exclude;
import static org.zalando.logbook.core.Conditions.requestTo;

@Configuration
@EnableWebSecurity
public class WebConfig {

    public static final String AUTH_PARAM_USERNAME = "email";
    public static final String AUTH_PARAM_PASSWORD = "password";
    private final SecurityContextRepository securityContextRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final LogoutSuccessHandler logoutSuccessHandler;

    public WebConfig(
            SecurityContextRepository securityContextRepository,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler,
            AuthenticationSuccessHandler authenticationSuccessHandler,
            AuthenticationFailureHandlerImpl authenticationFailureHandler,
            LogoutSuccessHandler logoutSuccessHandler
    ) {
        this.securityContextRepository = securityContextRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity

                .csrf(AbstractHttpConfigurer::disable)

                .headers(httpSecurityHeadersConfigurer ->
                        httpSecurityHeadersConfigurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(STATELESS))

                .securityContext(httpSecuritySecurityContextConfigurer ->
                        httpSecuritySecurityContextConfigurer.securityContextRepository(securityContextRepository))

                .exceptionHandling(httpSecurityExceptionHandlingConfigurer -> httpSecurityExceptionHandlingConfigurer
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                .formLogin(httpSecurityFormLoginConfigurer -> httpSecurityFormLoginConfigurer
                        .loginPage("/api/authentication")
                        .usernameParameter(AUTH_PARAM_USERNAME)
                        .passwordParameter(AUTH_PARAM_PASSWORD)
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler))

                .logout(httpSecurityLogoutConfigurer -> httpSecurityLogoutConfigurer
                        .logoutRequestMatcher(pathPattern(HttpMethod.DELETE, "/api/authentication"))
                        .logoutSuccessHandler(logoutSuccessHandler))

                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> authorizationManagerRequestMatcherRegistry
                        .requestMatchers("/api/installation/**").permitAll()
                        .requestMatchers("/api/file/**").hasAuthority(WebAuthority.FILE_API.name())
                        .requestMatchers(HttpMethod.GET, "/api/admin/llm/evaluation")
                        .access(allOf(hasAuthority(WebAuthority.FILE_API.name()), hasRole("ADMIN")))
                        .requestMatchers("/api/admin/**").hasAuthority(WebAuthority.ADMIN_API.name())
                        .requestMatchers("/api/**").hasAuthority(WebAuthority.USER_API.name())
                        .requestMatchers(OpenSubsonicResponseService.PATH_PREFIX + "/**").hasAuthority(WebAuthority.OPEN_SUBSONIC_API.name())
                        .anyRequest().permitAll()
                )

                .build();
    }

    @Bean
    public Logbook logbook() {
        return Logbook.builder()
                // StatusAtLeastStrategy still buffers bodies, so exclude bulk transfers before processing.
                .condition(exclude(
                        requestTo("/api/file/**"),
                        requestTo("/api/admin/llm/evaluation"),
                        requestTo("/api/admin/playlists/backup"),
                        requestTo("/api/admin/playlists/restore"),
                        requestTo("/api/admin/history/backup"),
                        requestTo("/api/admin/history/restore"),
                        requestTo(OpenSubsonicResponseService.PATH_PREFIX + "/rest/stream.view"),
                        requestTo(OpenSubsonicResponseService.PATH_PREFIX + "/rest/download.view"),
                        requestTo(OpenSubsonicResponseService.PATH_PREFIX + "/rest/getCoverArt.view")
                ))
                .strategy(new StatusAtLeastStrategy(400))
                .sink(new DefaultSink(new DefaultHttpLogFormatter(), new DefaultHttpLogWriter()))
                .build();
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCompressionCustomizer() {
        // Tomcat's sendfile path bypasses response compression for large static resources.
        return factory -> factory.addConnectorCustomizers(connector ->
                connector.setProperty("useSendfile", "false"));
    }
}
