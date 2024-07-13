package net.dorokhov.pony3.web.security;

import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.api.user.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserService userService;

    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.getByEmail(username).orElse(null);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("User '%s' not found.", username));
        }
        return new UserDetailsImpl(user);
    }
}
