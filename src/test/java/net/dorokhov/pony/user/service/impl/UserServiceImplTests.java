package net.dorokhov.pony.user.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.domain.UserToken;
import net.dorokhov.pony.user.repository.UserRepository;
import net.dorokhov.pony.user.service.command.CurrentUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.service.command.UserUpdateCommand;
import net.dorokhov.pony.user.service.exception.*;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTests {
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenSecretManager tokenSecretManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldInitToGetTokenSecret() throws Exception {
        userService.init();
        verify(tokenSecretManager).getTokenSecret();
        verify(tokenSecretManager, never()).generateAndStoreTokenSecret();
    }

    @Test
    public void shouldInitToGenerateTokenSecret() throws Exception {
        given(tokenSecretManager.getTokenSecret()).willThrow(new TokenSecretNotFoundException());
        userService.init();
        verify(tokenSecretManager).getTokenSecret();
        verify(tokenSecretManager).generateAndStoreTokenSecret();
    }

    @Test
    public void shouldGetById() throws Exception {
        given(userRepository.findOne(1L)).willReturn(null);
        assertThat(userService.getById(1L)).isNull();
        User user = userBuilder().build();
        given(userRepository.findOne(1L)).willReturn(user);
        assertThat(userService.getById(1L)).isSameAs(user);
    }

    @Test
    public void shouldGetByEmail() throws Exception {
        given(userRepository.findByEmail("someEmail")).willReturn(null);
        assertThat(userService.getByEmail("someEmail")).isNull();
        User user = userBuilder().build();
        given(userRepository.findByEmail("someEmail")).willReturn(user);
        assertThat(userService.getByEmail("someEmail")).isSameAs(user);
    }

    @Test
    public void shouldGetAll() throws Exception {
        Page<User> page = new PageImpl<>(ImmutableList.of());
        given(userRepository.findAll((Pageable) any())).willReturn(page);
        assertThat(userService.getAll(new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void shouldCreateUser() throws Exception {
        
        User createdUser = userBuilder().build();
        given(passwordEncoder.encode("somePassword")).willReturn("encodedPassword");
        given(userRepository.save((User) any())).willReturn(createdUser);

        UserCreationCommand command = UserCreationCommand.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .roles(User.Role.USER, User.Role.ADMIN)
                .build();
        assertThat(userService.create(command)).isSameAs(createdUser);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo("someName");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("someEmail");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedPassword");
        assertThat(userCaptor.getValue().getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
    }

    @Test
    public void shouldCreateExistingUser() throws Exception {
        User existingUser = userBuilder().build();
        given(userRepository.findByEmail("someEmail")).willReturn(existingUser);
        assertThatThrownBy(() -> userService.create(UserCreationCommand.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .build())).isInstanceOf(UserExistsException.class);        
    }

    @Test
    public void shouldUpdateUser() throws Exception {

        User existingUser = userBuilder().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        given(userRepository.save((User) any())).willReturn(existingUser);

        UserUpdateCommand command = UserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .roles(User.Role.USER, User.Role.ADMIN)
                .build();
        assertThat(userService.update(command)).isSameAs(existingUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(userCaptor.getValue().getName()).isEqualTo("someName");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("someEmail");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("somePassword");
        assertThat(userCaptor.getValue().getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
    }

    @Test
    public void shouldUpdateUserPassword() throws Exception {

        User existingUser = userBuilder().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        given(passwordEncoder.encode("somePassword")).willReturn("encodedPassword");

        UserUpdateCommand command = UserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .newPassword("somePassword")
                .build();
        userService.update(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    public void shouldUpdateNotFoundUser() throws Exception {
        given(userRepository.findOne(1L)).willReturn(null);
        UserUpdateCommand command = UserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .newPassword("somePassword")
                .build();
        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void shouldUpdateExistingUser() throws Exception {
        User userToUpdate = userBuilder().email("otherEmail").build();
        given(userRepository.findOne(1L)).willReturn(userToUpdate);
        User existingUser = userBuilder().id(2L).build();
        given(userRepository.findByEmail("someEmail")).willReturn(existingUser);
        UserUpdateCommand command = UserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .newPassword("somePassword")
                .build();
        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(UserExistsException.class);
    }

    @Test
    public void shouldDeleteUser() throws Exception {
        User existingUser = userBuilder().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        userService.delete(1L);
        verify(userRepository).delete(1L);
    }

    @Test
    public void shouldDeleteNotFoundUser() throws Exception {
        given(userRepository.findOne(1L)).willReturn(null);
        assertThatThrownBy(() -> userService.delete(1L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void shouldDeleteCurrentUser() throws Exception {
        User existingUser = userBuilder().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        assertThatThrownBy(() -> userService.delete(1L)).isInstanceOf(DeletingCurrentUserException.class);
    }

    @Test
    public void shouldAuthenticateValidCredentials() throws Exception {
        User existingUser = userBuilder().build();
        given(authenticationManager.authenticate(any())).willReturn(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        given(tokenSecretManager.getTokenSecret()).willReturn("someSecret");
        UserToken userToken = userService.authenticate("someEmail", "somePassword");
        assertThat(userToken.getUser()).isSameAs(existingUser);
        assertThat(userToken.getToken()).isNotNull();
        assertThat(userService.getCurrentUser()).isSameAs(existingUser);
    }

    @Test
    public void shouldAuthenticateInvalidCredentials() throws Exception {
        given(authenticationManager.authenticate(any()))
                .willThrow(new AuthenticationCredentialsNotFoundException("Credentials not found."));
        assertThatThrownBy(() -> userService.authenticate("someEmail", "somePassword")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    public void shouldAuthenticateValidToken() throws Exception {
        User existingUser = userBuilder().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        given(tokenSecretManager.getTokenSecret()).willReturn("someSecret");
        String token = JWT.create()
                .withSubject("1")
                .sign(Algorithm.HMAC256("someSecret"));
        assertThat(userService.authenticate(token)).isSameAs(existingUser);
        assertThat(userService.getCurrentUser()).isSameAs(existingUser);
    }

    @Test
    public void shouldAuthenticateInvalidToken() throws Exception {
        given(tokenSecretManager.getTokenSecret()).willReturn("someSecret");
        assertThatThrownBy(() -> userService.authenticate("invalidToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldAuthenticateValidTokenForNotExistingUser() throws Exception {
        given(userRepository.findOne(1L)).willReturn(null);
        given(tokenSecretManager.getTokenSecret()).willReturn("someSecret");
        String token = JWT.create()
                .withSubject("1")
                .sign(Algorithm.HMAC256("someSecret"));
        assertThatThrownBy(() -> userService.authenticate(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void shouldLogout() throws Exception {
        User existingUser = userBuilder().build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        assertThat(userService.logout()).isSameAs(existingUser);
    }

    @Test
    public void shouldLogoutNotAuthenticatedUser() throws Exception {
        assertThat(userService.logout()).isNull();
    }

    @Test
    public void shouldGetCurrentUserWhenNotAuthenticated() throws Exception {
        assertThat(userService.getCurrentUser()).isNull();
    }

    @Test
    public void shouldUpdateCurrentUser() throws Exception {

        given(passwordEncoder.matches(any(), any())).willReturn(true);
        given(passwordEncoder.encode("newPassword")).willReturn("encodedPassword");
        
        User existingUser = User.builder()
                .id(1L)
                .name("oldName")
                .email("oldEmail")
                .password("oldPassword")
                .addRoles(User.Role.USER, User.Role.ADMIN)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        
        given(userRepository.findOne(1L)).willReturn(existingUser);
        given(userRepository.save((User) any())).willReturn(existingUser);

        CurrentUserUpdateCommand command = CurrentUserUpdateCommand.builder()
                .name("someName")
                .email("someEmail")
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();
        assertThat(userService.updateCurrentUser(command)).isEqualTo(existingUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(userCaptor.getValue().getName()).isEqualTo("someName");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("someEmail");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedPassword");
        assertThat(userCaptor.getValue().getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
    }

    @Test
    public void shouldUpdateWhenNotAuthenticated() throws Exception {
        CurrentUserUpdateCommand command = CurrentUserUpdateCommand.builder()
                .name("someName")
                .email("someEmail")
                .oldPassword("oldPassword")
                .build();
        assertThatThrownBy(() -> userService.updateCurrentUser(command)).isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    public void shouldUpdateCurrentUserWithInvalidOldPassword() throws Exception {
        given(passwordEncoder.matches(any(), any())).willReturn(false);
        User existingUser = userBuilder().build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        CurrentUserUpdateCommand command = CurrentUserUpdateCommand.builder()
                .name("someName")
                .email("someEmail")
                .oldPassword("invalidPassword")
                .build();
        assertThatThrownBy(() -> userService.updateCurrentUser(command)).isInstanceOf(InvalidPasswordException.class);
    }
    
    private User.Builder userBuilder() {
        return User.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .password("somePassword");
    }
}
