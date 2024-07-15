package net.dorokhov.pony3.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.web.dto.ErrorDto;
import net.dorokhov.pony3.web.dto.UserDto;
import net.dorokhov.pony3.web.security.LogoutDelegate;
import net.dorokhov.pony3.web.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SecurityContextRepository securityContextRepository;
    private final MappingJackson2HttpMessageConverter messageConverter;
    private final List<LogoutDelegate> logoutDelegates;

    public LogoutSuccessHandlerImpl(
            SecurityContextRepository securityContextRepository,
            MappingJackson2HttpMessageConverter messageConverter, List<LogoutDelegate> logoutDelegates
    ) {
        this.securityContextRepository = securityContextRepository;
        this.messageConverter = messageConverter;
        this.logoutDelegates = logoutDelegates;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        SecurityContext securityContext = securityContextRepository.loadContext(new HttpRequestResponseHolder(request, response));
        User loggedOutUser = Optional.ofNullable(securityContext.getAuthentication())
                .filter(requestAuthentication -> requestAuthentication.getPrincipal() instanceof UserDetailsImpl)
                .map(requestAuthentication -> (UserDetailsImpl) requestAuthentication.getPrincipal())
                .map(UserDetailsImpl::getUser)
                .orElse(null);
        if (loggedOutUser != null) {
            logoutDelegates.forEach(logoutDelegate -> logoutDelegate.onLogout(loggedOutUser));
            logger.debug("User '{}' has logged out.");
            messageConverter.write(UserDto.of(loggedOutUser), MediaType.ALL, new ServletServerHttpResponse(response));
        } else {
            logger.debug("Logging out failed: user is not authenticated.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            messageConverter.write(ErrorDto.authenticationFailed(), MediaType.ALL, new ServletServerHttpResponse(response));
        }
    }
}
