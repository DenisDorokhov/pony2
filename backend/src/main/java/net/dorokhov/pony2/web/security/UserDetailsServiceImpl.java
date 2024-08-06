package net.dorokhov.pony2.web.security;

import jakarta.servlet.http.HttpServletRequest;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserService userService;
    private final BruteForceProtector bruteForceProtector;

    public UserDetailsServiceImpl(
            UserService userService,
            BruteForceProtector bruteForceProtector
    ) {
        this.userService = userService;
        this.bruteForceProtector = bruteForceProtector;
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        if (bruteForceProtector.shouldBlockLoginAttempt(request)) {
            throw new UsernameNotFoundException("Too many authentication attempts.");
        }
        User user = userService.getByEmail(username).orElse(null);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("User '%s' not found.", username));
        }
        return new UserDetailsImpl(user);
    }
}
