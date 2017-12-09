package net.dorokhov.pony.web.service;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.web.security.LogoutDelegate;
import org.springframework.stereotype.Component;

@Component
public class LogoutDelegateImpl implements LogoutDelegate {
    @Override
    public void onLogout(User user) {
        // Do nothing so far.
    }
}
