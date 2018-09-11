package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.controller.exception.BadRequestException;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.UserCreationCommandDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.domain.UserUpdateCommandDto;
import net.dorokhov.pony.web.service.UserFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static net.dorokhov.pony.web.controller.common.ApiResponseValues.*;

@RestController
@RequestMapping(value = "/api/admin/users", produces = "application/json")
@Api(tags = "User Administration")
@ApiResponses({
        @ApiResponse(code = UNAUTHORIZED_CODE, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
        @ApiResponse(code = FORBIDDEN_CODE, message = FORBIDDEN_MESSAGE, response = ErrorDto.class),
})
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
    @ApiResponses({
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested user not found.", response = ErrorDto.class),
    })
    public UserDto getUserById(@PathVariable String userId) throws ObjectNotFoundException {
        return userFacade.getUserById(userId);
    }
    
    @PostMapping
    @ApiOperation("Create user.")
    @ApiResponses({
            @ApiResponse(code = BAD_REQUEST_CODE, message = BAD_REQUEST_MESSAGE, response = ErrorDto.class),
    })
    public UserDto createUser(@Valid @RequestBody UserCreationCommandDto command) throws DuplicateEmailException {
        return userFacade.createUser(command);
    }
    
    @PutMapping("/{userId}")
    @ApiOperation("Update user.")
    @ApiResponses({
            @ApiResponse(code = BAD_REQUEST_CODE, message = BAD_REQUEST_MESSAGE, response = ErrorDto.class),
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested user not found.", response = ErrorDto.class),
    })
    public UserDto updateUser(@PathVariable String userId, @Valid @RequestBody UserUpdateCommandDto command) throws BadRequestException, ObjectNotFoundException, DuplicateEmailException {
        if (!userId.equals(command.getId())) {
            throw new BadRequestException();
        }
        return userFacade.updateUser(command);
    }
    
    @DeleteMapping("/{userId}")
    @ApiOperation("Delete user by ID.")
    @ApiResponses({
            @ApiResponse(code = BAD_REQUEST_CODE, message = "Deletion of current user not allowed.", response = ErrorDto.class),
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested user not found.", response = ErrorDto.class),
    })
    public UserDto deleteUser(@PathVariable String userId) throws BadRequestException, ObjectNotFoundException {
        if (userFacade.getCurrentUser().getId().equals(userId)) {
            throw new BadRequestException();
        }
        return userFacade.deleteUserById(userId);
    }
}
