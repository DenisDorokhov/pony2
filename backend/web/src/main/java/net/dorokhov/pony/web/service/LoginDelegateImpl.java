package net.dorokhov.pony.web.service;

import net.dorokhov.pony.api.user.domain.User;
import net.dorokhov.pony.web.security.LoginDelegate;
import org.springframework.stereotype.Component;

@Component
public class LoginDelegateImpl implements LoginDelegate {
    @Override
    public void onLogin(User user) {
        // Do nothing so far.
    }
}
