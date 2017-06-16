package net.dorokhov.pony.security.service.impl;

import net.dorokhov.pony.security.service.UserContextService;
import net.dorokhov.pony.security.service.exception.NotAuthenticatedException;
import net.dorokhov.pony.user.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return AuthenticationUtils.fetchUser(authentication);
        }
        return null;
    }
}
