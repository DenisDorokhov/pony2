package net.dorokhov.pony.security;

import net.dorokhov.pony.security.service.impl.web.AuthenticationFailureHandlerImpl;
import net.dorokhov.pony.security.service.impl.web.UnauthorizedEntryPoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static net.dorokhov.pony.user.domain.User.Role.ADMIN;
import static net.dorokhov.pony.user.domain.User.Role.USER;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final UnauthorizedEntryPoint unauthorizedEntryPoint;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final LogoutSuccessHandler logoutSuccessHandler;

    public SecurityConfig(UserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          SecurityContextRepository securityContextRepository,
                          UnauthorizedEntryPoint unauthorizedEntryPoint,
                          AuthenticationSuccessHandler authenticationSuccessHandler,
                          AuthenticationFailureHandlerImpl authenticationFailureHandler,
                          LogoutSuccessHandler logoutSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.unauthorizedEntryPoint = unauthorizedEntryPoint;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(STATELESS)

                .and()
                
                .securityContext()
                .securityContextRepository(securityContextRepository)
                
                .and()

                .exceptionHandling()
                .authenticationEntryPoint(unauthorizedEntryPoint)

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
                .antMatchers(HttpMethod.POST, "/api/installation").permitAll()
                .antMatchers(HttpMethod.POST, "/api/authentication").permitAll()
                .antMatchers(HttpMethod.DELETE, "/api/authentication").authenticated()
                .antMatchers("/api/admin/**").hasRole(ADMIN.name())
                .antMatchers("/api/**").hasRole(USER.name())
                .anyRequest().permitAll();
    }
}
