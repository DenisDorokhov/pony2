package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.security.service.UserContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController implements ErrorHandlingController {
    
    private final UserContextService userContextService;

    public UserController(UserContextService userContextService) {
        this.userContextService = userContextService;
    }

    @GetMapping
    public UserDto getCurrentUser() {
        return new UserDto(userContextService.getAuthenticatedUser());
    }
}
