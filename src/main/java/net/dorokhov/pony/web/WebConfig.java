package net.dorokhov.pony.web;

import net.dorokhov.pony.web.security.UnauthorizedEntryPoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import static net.dorokhov.pony.user.domain.User.Role.ADMIN;
import static net.dorokhov.pony.user.domain.User.Role.USER;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class WebConfig extends WebSecurityConfigurerAdapter {

    private final UnauthorizedEntryPoint unauthorizedEntryPoint;

    public WebConfig(UnauthorizedEntryPoint unauthorizedEntryPoint) {
        this.unauthorizedEntryPoint = unauthorizedEntryPoint;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(STATELESS).and()
                .exceptionHandling().authenticationEntryPoint(unauthorizedEntryPoint).and()
                .authorizeRequests()
                .antMatchers(
                        "/",
                        "/favicon.ico",
                        "/img/**",
                        "/css/**",
                        "/js/**",
                        "/api/installation",
                        "/api/authentication"
                ).permitAll()
                .antMatchers("/api/admin/**").hasRole(ADMIN.name())
                .anyRequest().hasRole(USER.name());
    }
}
