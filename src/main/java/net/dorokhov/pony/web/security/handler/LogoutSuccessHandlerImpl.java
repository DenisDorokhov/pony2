package net.dorokhov.pony.web.security.handler;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.security.UserDetailsImpl;
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SecurityContextRepository securityContextRepository;
    private final MappingJackson2HttpMessageConverter messageConverter;

    public LogoutSuccessHandlerImpl(SecurityContextRepository securityContextRepository,
                                    MappingJackson2HttpMessageConverter messageConverter) {
        this.securityContextRepository = securityContextRepository;
        this.messageConverter = messageConverter;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        SecurityContext securityContext = securityContextRepository.loadContext(new HttpRequestResponseHolder(request, response));
        User loggedOutUser = Optional.ofNullable(securityContext.getAuthentication())
                .filter(requestAuthentication -> requestAuthentication.getPrincipal() instanceof UserDetailsImpl)
                .map(requestAuthentication -> (UserDetailsImpl) requestAuthentication.getPrincipal())
                .map(UserDetailsImpl::getUser)
                .orElse(null);
        if (loggedOutUser != null) {
            logger.debug("User '{}' has logged out.");
            messageConverter.write(new UserDto(loggedOutUser), MediaType.ALL, new ServletServerHttpResponse(response));
        } else {
            logger.debug("Logging out failed: user is not authenticated.");
            response.setStatus(SC_UNAUTHORIZED);
            messageConverter.write(ErrorDto.accessDenied(), MediaType.ALL, new ServletServerHttpResponse(response));
        }
    }
}
