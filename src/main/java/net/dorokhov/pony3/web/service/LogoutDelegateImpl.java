package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.web.security.LogoutDelegate;
import org.springframework.stereotype.Component;

@Component
public class LogoutDelegateImpl implements LogoutDelegate {
    @Override
    public void onLogout(User user) {
        // Do nothing so far.
    }
}
