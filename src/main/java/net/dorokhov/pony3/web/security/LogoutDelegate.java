package net.dorokhov.pony3.web.security;

import net.dorokhov.pony3.api.user.domain.User;

public interface LogoutDelegate {
    void onLogout(User user);
}
