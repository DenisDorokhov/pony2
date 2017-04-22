package net.dorokhov.pony.user;

import net.dorokhov.pony.entity.User;

public class UserToken {
    
    private final User user;
    private final String token;

    public UserToken(User user, String token) {
        this.user = user;
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }
}
