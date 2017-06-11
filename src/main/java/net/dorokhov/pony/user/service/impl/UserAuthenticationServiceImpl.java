package net.dorokhov.pony.user.service.impl;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.domain.UserToken;
import net.dorokhov.pony.user.service.UserAuthenticationService;
import net.dorokhov.pony.user.service.exception.InvalidCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuthenticationServiceImpl implements UserAuthenticationService {
    
    private final TokenManager tokenManager;
    private final AuthenticationManager authenticationManager;

    public UserAuthenticationServiceImpl(TokenManager tokenManager, AuthenticationManager authenticationManager) {
        this.tokenManager = tokenManager;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional(readOnly = true)
    public UserToken authenticate(String email, String password) throws InvalidCredentialsException {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException(email);
        }
        User currentUser = ((UserDetailsImpl) authentication.getPrincipal()).getUser();
        return new UserToken(currentUser, tokenManager.signToken(currentUser.getId().toString()));
    }
}
