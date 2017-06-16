package net.dorokhov.pony.security.service.impl;

import net.dorokhov.pony.security.service.impl.userdetails.UserDetailsImpl;
import net.dorokhov.pony.user.domain.User;
import org.springframework.security.core.Authentication;

import javax.annotation.Nullable;

public final class AuthenticationUtils {

    private AuthenticationUtils() {
    }

    @Nullable
    public static User fetchUser(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getUser();
        }
        return null;
    }
}
