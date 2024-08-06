package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.web.security.UserDetailsImpl;
import net.dorokhov.pony2.web.service.exception.NotAuthenticatedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.Optional;

@Component
public class UserContext {
    
    public boolean isUserAuthenticated() {
        return doGetAuthenticatedUser() != null;
    }

    public User getAuthenticatedUser() throws NotAuthenticatedException {
        User user = doGetAuthenticatedUser();
        if (user == null) {
            throw new NotAuthenticatedException();
        }
        return user;
    }
    
    @Nullable
    private User doGetAuthenticatedUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getPrincipal() instanceof UserDetailsImpl)
                .map(requestAuthentication -> (UserDetailsImpl) requestAuthentication.getPrincipal())
                .map(UserDetailsImpl::getUser)
                .orElse(null);
    }
}
