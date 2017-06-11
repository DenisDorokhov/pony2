package net.dorokhov.pony.user.service;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.exception.InvalidTokenException;
import net.dorokhov.pony.user.service.exception.NotAuthenticatedException;

import javax.annotation.Nullable;

public interface CurrentUserService {
    
    @Nullable
    User getCurrentUser();
    
    User authenticate(String token) throws InvalidTokenException;

    User logout() throws NotAuthenticatedException;
}
