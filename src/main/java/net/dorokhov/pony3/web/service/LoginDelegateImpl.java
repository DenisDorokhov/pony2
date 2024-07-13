package net.dorokhov.pony3.web.service;

import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.web.security.LoginDelegate;
import org.springframework.stereotype.Component;

@Component
public class LoginDelegateImpl implements LoginDelegate {
    @Override
    public void onLogin(User user) {
        // Do nothing so far.
    }
}
