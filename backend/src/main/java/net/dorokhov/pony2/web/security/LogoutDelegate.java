package net.dorokhov.pony2.web.security;

import net.dorokhov.pony2.api.user.domain.User;

public interface LogoutDelegate {
    void onLogout(User user);
}
