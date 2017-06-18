package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.web.controller.exception.BadRequestException;
import net.dorokhov.pony.web.domain.UserCreationCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.domain.UserUpdateCommandDto;
import net.dorokhov.pony.web.service.UserFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController implements ErrorHandlingController {
    
    private final UserFacade userFacade;

    public UserAdminController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }
    
    @GetMapping
    public List<UserDto> getAllUsers() {
        return userFacade.getAllUsers();
    }
    
    @GetMapping("/{userId}")
    public UserDto getUserById(@PathVariable Long userId) throws ObjectNotFoundException {
        return userFacade.getUserById(userId);
    }
    
    @PostMapping
    public UserDto createUser(@Valid @RequestBody UserCreationCommandDto command) throws DuplicateEmailException {
        return userFacade.createUser(command);
    }
    
    @PutMapping("/{userId}")
    public UserDto updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateCommandDto command) throws BadRequestException, ObjectNotFoundException, DuplicateEmailException {
        if (!userId.equals(command.getId())) {
            throw new BadRequestException();
        }
        return userFacade.updateUser(command);
    }
    
    @DeleteMapping("/{userId}")
    public UserDto deleteUser(@PathVariable Long userId) throws BadRequestException, ObjectNotFoundException {
        if (userFacade.getCurrentUser().getId().equals(userId)) {
            throw new BadRequestException();
        }
        return userFacade.deleteUserById(userId);
    }
}
