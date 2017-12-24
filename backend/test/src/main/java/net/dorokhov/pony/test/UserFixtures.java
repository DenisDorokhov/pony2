package net.dorokhov.pony.test;

import net.dorokhov.pony.api.user.domain.User;

public final class UserFixtures {

    private UserFixtures() {
    }
    
    public static User user() {
        return userBuilder().build();
    }
    
    public static User.Builder userBuilder() {
        return User.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .password("somePassword");
    }
}
