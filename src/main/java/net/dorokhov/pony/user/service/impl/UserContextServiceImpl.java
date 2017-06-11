package net.dorokhov.pony.user.service.impl;

import com.google.common.primitives.Longs;
import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserContextService;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.exception.InvalidTokenException;
import net.dorokhov.pony.user.service.exception.NotAuthenticatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class UserContextServiceImpl implements UserContextService {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TokenManager tokenManager;
    private final UserService userService;

    public UserContextServiceImpl(TokenManager tokenManager, UserService userService) {
        this.tokenManager = tokenManager;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public User getUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(authentication -> authentication.getPrincipal() instanceof UserDetailsImpl)
                .map(authentication -> (UserDetailsImpl) authentication.getPrincipal())
                .map(UserDetailsImpl::getUser)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User setUserFromToken(String token) throws InvalidTokenException {
        Long userId = Longs.tryParse(tokenManager.verifyToken(token));
        if (userId == null) {
            throw new InvalidTokenException();
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new InvalidTokenException();
        }
        logger.debug("Authenticating user '{}'.", user.getId());
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return user;
    }

    public User clearUser() throws NotAuthenticatedException {
        User user = getUser();
        if (user == null) {
            throw new NotAuthenticatedException();
        }
        logger.debug("Logging out user '{}'.", user.getId());
        SecurityContextHolder.clearContext();
        return user;
    }
}
