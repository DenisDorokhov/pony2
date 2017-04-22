package net.dorokhov.pony.user;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.entity.User;
import net.dorokhov.pony.repository.UserRepository;
import net.dorokhov.pony.user.domain.CurrentUserUpdateDraft;
import net.dorokhov.pony.user.domain.UserCreationDraft;
import net.dorokhov.pony.user.domain.UserToken;
import net.dorokhov.pony.user.domain.UserUpdateDraft;
import net.dorokhov.pony.user.exception.*;
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
    public void getById() throws Exception {
        given(userRepository.findOne(1L)).willReturn(null);
        assertThat(userService.getById(1L)).isEmpty();
        User user = buildUser().build();
        given(userRepository.findOne(1L)).willReturn(user);
        assertThat(userService.getById(1L)).hasValue(user);
    }

    @Test
    public void getByEmail() throws Exception {
        given(userRepository.findByEmail("someEmail")).willReturn(null);
        assertThat(userService.getByEmail("someEmail")).isEmpty();
        User user = buildUser().build();
        given(userRepository.findByEmail("someEmail")).willReturn(user);
        assertThat(userService.getByEmail("someEmail")).hasValue(user);
    }

    @Test
    public void getAll() throws Exception {
        Page<User> page = new PageImpl<>(ImmutableList.of());
        given(userRepository.findAll((Pageable) any())).willReturn(page);
        assertThat(userService.getAll(new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void createUser() throws Exception {
        
        User createdUser = buildUser().build();
        given(passwordEncoder.encode("somePassword")).willReturn("encodedPassword");
        given(userRepository.save((User) any())).willReturn(createdUser);

        UserCreationDraft draft = UserCreationDraft.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .roles(User.Role.USER, User.Role.ADMIN)
                .build();
        assertThat(userService.create(draft)).isSameAs(createdUser);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo("someName");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("someEmail");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedPassword");
        assertThat(userCaptor.getValue().getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
    }

    @Test
    public void createExistingUser() throws Exception {
        User existingUser = buildUser().build();
        given(userRepository.findByEmail("someEmail")).willReturn(existingUser);
        assertThatThrownBy(() -> userService.create(UserCreationDraft.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .build())).isInstanceOf(UserExistsException.class);        
    }

    @Test
    public void updateUser() throws Exception {

        User existingUser = buildUser().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        given(userRepository.save((User) any())).willReturn(existingUser);

        UserUpdateDraft draft = UserUpdateDraft.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .roles(User.Role.USER, User.Role.ADMIN)
                .build();
        assertThat(userService.update(draft)).isSameAs(existingUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(userCaptor.getValue().getName()).isEqualTo("someName");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("someEmail");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("somePassword");
        assertThat(userCaptor.getValue().getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
    }

    @Test
    public void updateUserPassword() throws Exception {

        User existingUser = buildUser().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        given(passwordEncoder.encode("somePassword")).willReturn("encodedPassword");

        UserUpdateDraft draft = UserUpdateDraft.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .newPassword("somePassword")
                .build();
        userService.update(draft);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    public void updateNotFoundUser() throws Exception {
        given(userRepository.findOne(1L)).willReturn(null);
        UserUpdateDraft draft = UserUpdateDraft.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .newPassword("somePassword")
                .build();
        assertThatThrownBy(() -> userService.update(draft)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void updateExistingUser() throws Exception {
        User userToUpdate = buildUser().email("otherEmail").build();
        given(userRepository.findOne(1L)).willReturn(userToUpdate);
        User existingUser = buildUser().id(2L).build();
        given(userRepository.findByEmail("someEmail")).willReturn(existingUser);
        UserUpdateDraft draft = UserUpdateDraft.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .newPassword("somePassword")
                .build();
        assertThatThrownBy(() -> userService.update(draft)).isInstanceOf(UserExistsException.class);
    }

    @Test
    public void deleteUser() throws Exception {
        User existingUser = buildUser().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        userService.delete(1L);
        verify(userRepository).delete(1L);
    }

    @Test
    public void deleteNotFoundUser() throws Exception {
        given(userRepository.findOne(1L)).willReturn(null);
        assertThatThrownBy(() -> userService.delete(1L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void deleteCurrentUser() throws Exception {
        User existingUser = buildUser().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        assertThatThrownBy(() -> userService.delete(1L)).isInstanceOf(DeletingCurrentUserException.class);
    }

    @Test
    public void authenticateValidCredentials() throws Exception {
        User existingUser = buildUser().build();
        given(authenticationManager.authenticate(any())).willReturn(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        given(tokenSecretManager.getTokenSecret()).willReturn("someSecret");
        UserToken userToken = userService.authenticate("someEmail", "somePassword");
        assertThat(userToken.getUser()).isSameAs(existingUser);
        assertThat(userToken.getToken()).isNotNull();
        assertThat(userService.getCurrentUser()).hasValue(existingUser);
    }

    @Test
    public void authenticateInvalidCredentials() throws Exception {
        given(authenticationManager.authenticate(any()))
                .willThrow(new AuthenticationCredentialsNotFoundException("Credentials not found."));
        assertThatThrownBy(() -> userService.authenticate("someEmail", "somePassword")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    public void authenticateValidToken() throws Exception {
        User existingUser = buildUser().build();
        given(userRepository.findOne(1L)).willReturn(existingUser);
        given(tokenSecretManager.getTokenSecret()).willReturn("someSecret");
        String token = JWT.create()
                .withSubject("1")
                .sign(Algorithm.HMAC256("someSecret"));
        assertThat(userService.authenticate(token)).isSameAs(existingUser);
        assertThat(userService.getCurrentUser()).hasValue(existingUser);
    }

    @Test
    public void authenticateInvalidToken() throws Exception {
        given(tokenSecretManager.getTokenSecret()).willReturn("someSecret");
        assertThatThrownBy(() -> userService.authenticate("invalidToken")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void authenticateValidTokenForNotExistingUser() throws Exception {
        given(userRepository.findOne(1L)).willReturn(null);
        given(tokenSecretManager.getTokenSecret()).willReturn("someSecret");
        String token = JWT.create()
                .withSubject("1")
                .sign(Algorithm.HMAC256("someSecret"));
        assertThatThrownBy(() -> userService.authenticate(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void logout() throws Exception {
        User existingUser = buildUser().build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        assertThat(userService.logout()).hasValue(existingUser);
    }

    @Test
    public void logoutNotAuthenticatedUser() throws Exception {
        assertThat(userService.logout()).isEmpty();
    }

    @Test
    public void getCurrentUserWhenNotAuthenticated() throws Exception {
        assertThat(userService.getCurrentUser()).isEmpty();
    }

    @Test
    public void updateCurrentUser() throws Exception {

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

        CurrentUserUpdateDraft draft = CurrentUserUpdateDraft.builder()
                .name("someName")
                .email("someEmail")
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();
        assertThat(userService.updateCurrentUser(draft)).isSameAs(existingUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(userCaptor.getValue().getName()).isEqualTo("someName");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("someEmail");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedPassword");
        assertThat(userCaptor.getValue().getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
    }

    @Test
    public void updateWhenNotAuthenticated() throws Exception {
        CurrentUserUpdateDraft draft = CurrentUserUpdateDraft.builder()
                .name("someName")
                .email("someEmail")
                .oldPassword("oldPassword")
                .build();
        assertThatThrownBy(() -> userService.updateCurrentUser(draft)).isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    public void updateCurrentUserWithInvalidOldPassword() throws Exception {
        given(passwordEncoder.matches(any(), any())).willReturn(false);
        User existingUser = buildUser().build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserDetailsImpl(existingUser), null, null));
        CurrentUserUpdateDraft draft = CurrentUserUpdateDraft.builder()
                .name("someName")
                .email("someEmail")
                .oldPassword("invalidPassword")
                .build();
        assertThatThrownBy(() -> userService.updateCurrentUser(draft)).isInstanceOf(InvalidPasswordException.class);
    }
    
    private User.Builder buildUser() {
        return User.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .password("somePassword");
    }
}
