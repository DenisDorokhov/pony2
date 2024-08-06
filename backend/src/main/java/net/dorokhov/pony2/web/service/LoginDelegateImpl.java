package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.web.security.LoginDelegate;
import org.springframework.stereotype.Component;

@Component
public class LoginDelegateImpl implements LoginDelegate {
    @Override
    public void onLogin(User user) {
        // Do nothing so far.
    }
}
