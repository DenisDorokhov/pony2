package net.dorokhov.pony.web.controller;

import java.util.List;
import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.dorokhov.pony.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.web.controller.exception.BadRequestException;
import net.dorokhov.pony.web.domain.UserCreationCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.domain.UserUpdateCommandDto;
import net.dorokhov.pony.web.service.UserFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/users", produces = "application/json")
@Api(tags = "User Administration")
public class UserAdminController implements ErrorHandlingController {
    
    private final UserFacade userFacade;

    public UserAdminController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }
    
    @GetMapping
    @ApiOperation("Get list of users.")
    public List<UserDto> getAllUsers() {
        return userFacade.getAllUsers();
    }
    
    @GetMapping("/{userId}")
    @ApiOperation("Get user by ID.")
    public UserDto getUserById(@PathVariable Long userId) throws ObjectNotFoundException {
        return userFacade.getUserById(userId);
    }
    
    @PostMapping
    @ApiOperation("Create user.")
    public UserDto createUser(@Valid @RequestBody UserCreationCommandDto command) throws DuplicateEmailException {
        return userFacade.createUser(command);
    }
    
    @PutMapping("/{userId}")
    @ApiOperation("Update user.")
    public UserDto updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateCommandDto command) throws BadRequestException, ObjectNotFoundException, DuplicateEmailException {
        if (!userId.equals(command.getId())) {
            throw new BadRequestException();
        }
        return userFacade.updateUser(command);
    }
    
    @DeleteMapping("/{userId}")
    @ApiOperation("Delete user by ID.")
    public UserDto deleteUser(@PathVariable Long userId) throws BadRequestException, ObjectNotFoundException {
        if (userFacade.getCurrentUser().getId().equals(userId)) {
            throw new BadRequestException();
        }
        return userFacade.deleteUserById(userId);
    }
}
