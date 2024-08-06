package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.user.domain.User;
import org.springframework.data.domain.Page;

import java.util.List;

public class UserPageDto extends PageDto<UserPageDto> {

    private List<UserDto> users;

    public List<UserDto> getUsers() {
        return users;
    }

    public UserPageDto setUsers(List<UserDto> users) {
        this.users = users;
        return this;
    }

    public static UserPageDto of(Page<User> userPage) {
        return new UserPageDto()
                .setPageIndex(userPage.getNumber())
                .setPageSize(userPage.getSize())
                .setTotalPages(userPage.getTotalPages())
                .setUsers(userPage.getContent().stream()
                        .map(UserDto::of)
                        .toList());
    }
}
