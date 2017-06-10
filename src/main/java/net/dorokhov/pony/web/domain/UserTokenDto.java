package net.dorokhov.pony.web.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import net.dorokhov.pony.user.domain.UserToken;

public final class UserTokenDto {
    
    private final UserDto user;
    private final String token;

    @JsonCreator
    public UserTokenDto(UserDto user, String token) {
        this.user = user;
        this.token = token;
    }
    
    public UserTokenDto(UserToken userToken) {
        this(new UserDto(userToken.getUser()), userToken.getToken());
    }

    public UserDto getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }
}
