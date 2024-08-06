package net.dorokhov.pony2.web.security;

import net.dorokhov.pony2.api.user.domain.User;

public interface LoginDelegate {
    void onLogin(User user);
}
