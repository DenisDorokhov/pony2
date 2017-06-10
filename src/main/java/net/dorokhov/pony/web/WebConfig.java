package net.dorokhov.pony.web;

import net.dorokhov.pony.web.security.UnauthorizedEntryPoint;
import net.dorokhov.pony.web.security.UserTokenFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static net.dorokhov.pony.user.domain.User.Role.ADMIN;
import static net.dorokhov.pony.user.domain.User.Role.USER;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class WebConfig extends WebSecurityConfigurerAdapter {

    private final UnauthorizedEntryPoint unauthorizedEntryPoint;
    private final UserTokenFilter userTokenFilter;

    public WebConfig(UnauthorizedEntryPoint unauthorizedEntryPoint, @Lazy UserTokenFilter userTokenFilter) {
        this.unauthorizedEntryPoint = unauthorizedEntryPoint;
        this.userTokenFilter = userTokenFilter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(STATELESS)
                .and()
                .exceptionHandling().authenticationEntryPoint(unauthorizedEntryPoint)
                .and()
                .addFilterBefore(userTokenFilter, UsernamePasswordAuthenticationFilter.class)
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
