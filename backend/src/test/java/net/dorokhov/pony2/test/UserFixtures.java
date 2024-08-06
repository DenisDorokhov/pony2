package net.dorokhov.pony2.test;

import net.dorokhov.pony2.api.user.domain.User;

public final class UserFixtures {

    private UserFixtures() {
    }
    
    public static User user() {
        return new User()
                .setId("1")
                .setName("someName")
                .setEmail("someEmail")
                .setPassword("somePassword");
    }
}
