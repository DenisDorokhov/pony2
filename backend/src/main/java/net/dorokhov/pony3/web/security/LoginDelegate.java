package net.dorokhov.pony3.web.security;

import net.dorokhov.pony3.api.user.domain.User;

public interface LoginDelegate {
    void onLogin(User user);
}
