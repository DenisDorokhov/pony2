package net.dorokhov.pony.user.service.impl;

import com.google.common.primitives.Longs;
import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.repository.UserRepository;
import net.dorokhov.pony.user.service.CurrentUserService;
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
public class CurrentUserServiceImpl implements CurrentUserService {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TokenManager tokenManager;
    private final UserRepository userRepository;

    public CurrentUserServiceImpl(TokenManager tokenManager, UserRepository userRepository) {
        this.tokenManager = tokenManager;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public User getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(authentication -> authentication.getPrincipal() instanceof UserDetailsImpl)
                .map(authentication -> (UserDetailsImpl) authentication.getPrincipal())
                .map(UserDetailsImpl::getUser)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(String token) throws InvalidTokenException {
        Long userId = Longs.tryParse(tokenManager.verifyToken(token));
        if (userId == null) {
            throw new InvalidTokenException();
        }
        User user = userRepository.findOne(userId);
        if (user == null) {
            throw new InvalidTokenException();
        }
        logger.debug("Authenticating user '{}'.", user.getId());
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return user;
    }

    public User logout() throws NotAuthenticatedException {
        User user = getCurrentUser();
        if (user == null) {
            throw new NotAuthenticatedException();
        }
        logger.debug("Logging out user '{}'.", user.getId());
        SecurityContextHolder.clearContext();
        return user;
    }
}
