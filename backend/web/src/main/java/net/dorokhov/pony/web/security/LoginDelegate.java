package net.dorokhov.pony.web.security;

import net.dorokhov.pony.api.user.domain.User;

public interface LoginDelegate {
    void onLogin(User user);
}
