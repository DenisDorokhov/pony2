package net.dorokhov.pony.security.service;

import net.dorokhov.pony.security.service.exception.NotAuthenticatedException;
import net.dorokhov.pony.user.domain.User;

public interface UserContextService {
    
    boolean isUserAuthenticated();
    
    User getAuthenticatedUser() throws NotAuthenticatedException;
}
