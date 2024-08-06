package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.web.security.LogoutDelegate;
import org.springframework.stereotype.Component;

@Component
public class LogoutDelegateImpl implements LogoutDelegate {
    @Override
    public void onLogout(User user) {
        // Do nothing so far.
    }
}
