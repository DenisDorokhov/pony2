package net.dorokhov.pony2.web.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.dto.UserDto;
import net.dorokhov.pony2.web.security.LogoutDelegate;
import net.dorokhov.pony2.web.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SecurityContextRepository securityContextRepository;
    private final JsonMapper jsonMapper;
    private final List<LogoutDelegate> logoutDelegates;

    public LogoutSuccessHandlerImpl(
            SecurityContextRepository securityContextRepository,
            JsonMapper jsonMapper, List<LogoutDelegate> logoutDelegates
    ) {
        this.securityContextRepository = securityContextRepository;
        this.jsonMapper = jsonMapper;
        this.logoutDelegates = logoutDelegates;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        SecurityContext securityContext = securityContextRepository.loadDeferredContext(request).get();
        User loggedOutUser = Optional.ofNullable(securityContext.getAuthentication())
                .filter(requestAuthentication -> requestAuthentication.getPrincipal() instanceof UserDetailsImpl)
                .map(requestAuthentication -> (UserDetailsImpl) requestAuthentication.getPrincipal())
                .map(UserDetailsImpl::getUser)
                .orElse(null);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (loggedOutUser != null) {
            logoutDelegates.forEach(logoutDelegate -> logoutDelegate.onLogout(loggedOutUser));
            logger.debug("User '{}' has logged out.", loggedOutUser.getEmail());
            jsonMapper.writeValue(response.getOutputStream(), UserDto.of(loggedOutUser));
        } else {
            logger.debug("Logging out failed: user is not authenticated.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonMapper.writeValue(response.getOutputStream(), ErrorDto.authenticationFailed());
        }
    }
}
