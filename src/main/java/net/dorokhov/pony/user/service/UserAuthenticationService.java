package net.dorokhov.pony.user.service;

import net.dorokhov.pony.user.domain.UserToken;
import net.dorokhov.pony.user.service.exception.InvalidCredentialsException;

public interface UserAuthenticationService {
    UserToken authenticate(String email, String password) throws InvalidCredentialsException;
}
