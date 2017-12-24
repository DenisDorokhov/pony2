package net.dorokhov.pony.web.security;

import net.dorokhov.pony.api.user.domain.User;

public interface LogoutDelegate {
    void onLogout(User user);
}
