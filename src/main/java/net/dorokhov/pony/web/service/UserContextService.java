package net.dorokhov.pony.web.service;

import net.dorokhov.pony.web.service.exception.NotAuthenticatedException;
import net.dorokhov.pony.user.domain.User;

public interface UserContextService {
    
    boolean isUserAuthenticated();
    
    User getAuthenticatedUser() throws NotAuthenticatedException;
}
