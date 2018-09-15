package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.domain.CurrentUserUpdateCommandDto;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.service.UserFacade;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.BAD_REQUEST_MESSAGE;
import static net.dorokhov.pony.web.controller.common.SwaggerResponses.UNAUTHORIZED_MESSAGE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/user", produces = APPLICATION_JSON_VALUE)
@Api(tags = "User")
@ApiResponses({
        @ApiResponse(code = SC_UNAUTHORIZED, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
})
public class UserController implements ErrorHandlingController {
    
    private final UserFacade userFacade;

    public UserController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @GetMapping
    @ApiOperation("Get current user.")
    public UserDto getCurrentUser() {
        return userFacade.getCurrentUser();
    }
    
    @PutMapping
    @ApiOperation("Update current user.")
    @ApiResponses({
            @ApiResponse(code = SC_BAD_REQUEST, message = BAD_REQUEST_MESSAGE, response = ErrorDto.class),
    })
    public UserDto updateCurrentUser(@Valid @RequestBody CurrentUserUpdateCommandDto command) throws InvalidPasswordException, DuplicateEmailException {
        return userFacade.updateCurrentUser(command);
    }
}
