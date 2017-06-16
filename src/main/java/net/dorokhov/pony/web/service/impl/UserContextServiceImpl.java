package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.web.security.UserDetailsImpl;
import net.dorokhov.pony.web.service.UserContextService;
import net.dorokhov.pony.web.service.exception.NotAuthenticatedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Optional;

@Component
public class UserContextServiceImpl implements UserContextService {
    
    @Override
    public boolean isUserAuthenticated() {
        return doGetAuthenticatedUser() != null;
    }

    @Override
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
