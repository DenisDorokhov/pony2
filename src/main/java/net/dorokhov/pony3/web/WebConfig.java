package net.dorokhov.pony3.web;

import net.dorokhov.pony3.web.security.WebAuthority;
import net.dorokhov.pony3.web.security.handler.AuthenticationFailureHandlerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class WebConfig {

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
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/*", "/assets/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf().disable()
                .headers().frameOptions().sameOrigin()
                .and()

                .sessionManagement().sessionCreationPolicy(STATELESS)
                .and()

                .securityContext()
                .securityContextRepository(securityContextRepository)
                .and()

                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .and()

                .formLogin()
                .loginPage("/api/authentication")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
                .and()

                .logout()
                .logoutRequestMatcher(
                        new AntPathRequestMatcher(
                                "/api/authentication",
                                HttpMethod.DELETE.name()))
                .logoutSuccessHandler(logoutSuccessHandler)
                .and()

                .authorizeRequests()
                .requestMatchers("/api/installation/**").permitAll()
                .requestMatchers("/api/file/**").hasAuthority(WebAuthority.FILE_API.name())
                .requestMatchers("/api/admin/**").hasAuthority(WebAuthority.ADMIN_API.name())
                .requestMatchers("/api/**").hasAuthority(WebAuthority.USER_API.name())
                .anyRequest().permitAll();
        return httpSecurity.build();
    }
}
