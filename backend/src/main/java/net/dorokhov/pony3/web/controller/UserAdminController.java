package net.dorokhov.pony3.web.controller;

import jakarta.validation.Valid;
import net.dorokhov.pony3.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony3.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony3.web.controller.exception.BadRequestException;
import net.dorokhov.pony3.web.dto.UserCreationCommandDto;
import net.dorokhov.pony3.web.dto.UserDto;
import net.dorokhov.pony3.web.dto.UserPageDto;
import net.dorokhov.pony3.web.dto.UserUpdateCommandDto;
import net.dorokhov.pony3.web.service.UserFacade;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class UserAdminController implements ErrorHandlingController {
    
    private final UserFacade userFacade;

    public UserAdminController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }
    
    @GetMapping("/api/admin/users")
    public UserPageDto getAllUsers(@RequestParam(defaultValue = "0") int pageIndex, @RequestParam(defaultValue = "30") int pageSize) {
        return userFacade.getAllUsers(pageIndex, pageSize);
    }
    
    @GetMapping("/api/admin/users/{userId}")
    public UserDto getUserById(@PathVariable String userId) throws ObjectNotFoundException {
        return userFacade.getUserById(userId);
    }
    
    @PostMapping("/api/admin/users")
    public UserDto createUser(@Valid @RequestBody UserCreationCommandDto command) throws DuplicateEmailException {
        return userFacade.createUser(command);
    }
    
    @PutMapping("/api/admin/users/{userId}")
    public UserDto updateUser(@PathVariable String userId, @Valid @RequestBody UserUpdateCommandDto command) throws BadRequestException, ObjectNotFoundException, DuplicateEmailException {
        if (!userId.equals(command.getId())) {
            throw new BadRequestException();
        }
        return userFacade.updateUser(command);
    }
    
    @DeleteMapping("/api/admin/users/{userId}")
    public UserDto deleteUser(@PathVariable String userId) throws BadRequestException, ObjectNotFoundException {
        if (userFacade.getCurrentUser().getId().equals(userId)) {
            throw new BadRequestException();
        }
        return userFacade.deleteUserById(userId);
    }
}
