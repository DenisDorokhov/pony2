package net.dorokhov.pony.web.security;

import net.dorokhov.pony.user.domain.User;

public interface LogoutDelegate {
    void onLogout(User user);
}
