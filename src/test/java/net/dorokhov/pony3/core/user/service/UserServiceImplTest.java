package net.dorokhov.pony3.core.user.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.api.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony3.api.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony3.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony3.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony3.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony3.api.user.service.exception.UserNotFoundException;
import net.dorokhov.pony3.core.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static net.dorokhov.pony3.test.UserFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @InjectMocks
    @Spy
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    public void shouldGetById() {

        User user = user();
        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        assertThat(userService.getById("1")).containsSame(user);
    }

    @Test
    public void shouldGetByEmail() {

        User user = user();
        when(userRepository.findByEmail("someEmail")).thenReturn(Optional.of(user));

        assertThat(userService.getByEmail("someEmail")).containsSame(user);
    }

    @Test
    public void shouldGetAll() {

        List<User> users = ImmutableList.of(user());
        when(userRepository.findAll((Sort) any())).thenReturn(users);

        assertThat(userService.getAll()).isSameAs(users);
    }

    @Test
    public void shouldCheckUserPassword() throws UserNotFoundException {

        User existingUser = user();
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThat(userService.checkUserPassword(existingUser.getId(), "somePassword")).isTrue();
    }

    @Test
    public void shouldFailUserPasswordCheckIfUserNotFound() {

        when(userRepository.findById("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.checkUserPassword("1", "somePassword")).isInstanceOfSatisfying(UserNotFoundException.class, e ->
                assertThat(e.getId()).isEqualTo("1"));
    }

    @Test
    public void shouldFailUserPasswordCheckIfPasswordDoesNotMatch() throws UserNotFoundException {

        User existingUser = user();
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThat(userService.checkUserPassword("1", "somePassword")).isFalse();
    }

    @Test
    public void shouldCreateUser() throws DuplicateEmailException {

        when(passwordEncoder.encode("somePassword")).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenAnswer(returnsFirstArg());
        UserCreationCommand command = new UserCreationCommand()
                .setName("someName")
                .setEmail("someEmail")
                .setPassword("somePassword")
                .setRoles(Sets.newHashSet(User.Role.USER, User.Role.ADMIN));

        User createdUser = userService.create(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).satisfies(user -> {
            assertThat(createdUser).isSameAs(user);
            assertThat(user.getName()).isEqualTo("someName");
            assertThat(user.getEmail()).isEqualTo("someEmail");
            assertThat(user.getPassword()).isEqualTo("encodedPassword");
            assertThat(user.getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
        });
    }

    @Test
    public void shouldFailUserCreationIfUserExists() {

        User existingUser = user();
        when(userRepository.findByEmail("someEmail")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.create(new UserCreationCommand()
                .setName("someName")
                .setEmail("someEmail")
                .setPassword("somePassword")
        )).isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    public void shouldUpdateUserUnsafely() throws UserNotFoundException, DuplicateEmailException {

        User existingUser = user();
        when(userRepository.findById("1")).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());
        UnsafeUserUpdateCommand command = new UnsafeUserUpdateCommand()
                .setId("1")
                .setName("someName")
                .setEmail("someEmail")
                .setRoles(Sets.newHashSet(User.Role.USER, User.Role.ADMIN));

        User updatedUser = userService.update(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue()).satisfies(user -> {
            assertThat(updatedUser).isSameAs(user);
            assertThat(user.getId()).isEqualTo("1");
            assertThat(user.getName()).isEqualTo("someName");
            assertThat(user.getEmail()).isEqualTo("someEmail");
            assertThat(user.getPassword()).isEqualTo("somePassword");
            assertThat(user.getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
        });
    }

    @Test
    public void shouldUpdateUserPasswordUnsafely() throws UserNotFoundException, DuplicateEmailException {

        User existingUser = user();
        when(userRepository.findById("1")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(existingUser.getEmail())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());
        UnsafeUserUpdateCommand command = new UnsafeUserUpdateCommand()
                .setId(existingUser.getId())
                .setName(existingUser.getName())
                .setEmail(existingUser.getEmail())
                .setNewPassword("newPassword");

        User updatedUser = userService.update(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue()).satisfies(user -> {
            assertThat(updatedUser).isSameAs(user);
            assertThat(user.getPassword()).isEqualTo("encodedPassword");
        });
    }

    @Test
    public void shouldFailUnsafeUserUpdateIfUserNotFound() {

        when(userRepository.findById("1")).thenReturn(Optional.empty());
        UnsafeUserUpdateCommand command = new UnsafeUserUpdateCommand()
                .setId("1")
                .setName("someName")
                .setEmail("someEmail")
                .setNewPassword("somePassword");

        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void shouldFailUnsafeUserUpdateIfNewUserEmailExists() {

        User userToUpdate = user().setEmail("otherEmail");
        when(userRepository.findById("1")).thenReturn(Optional.of(userToUpdate));
        User existingUser = user().setId("2");
        when(userRepository.findByEmail("someEmail")).thenReturn(Optional.of(existingUser));
        UnsafeUserUpdateCommand command = new UnsafeUserUpdateCommand()
                .setId("1")
                .setName("someName")
                .setEmail("someEmail")
                .setNewPassword("somePassword");

        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    public void shouldDeleteUser() throws UserNotFoundException {

        User existingUser = user();
        when(userRepository.findById("1")).thenReturn(Optional.of(existingUser));

        userService.delete("1");

        verify(userRepository).deleteById("1");
    }

    @Test
    public void shouldFailUserDeletionIdUserNotFound() {

        when(userRepository.findById("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete("1")).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void shouldUpdateUserSafely() throws UserNotFoundException, InvalidPasswordException, DuplicateEmailException {

        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");
        User existingUser = new User()
                .setId("1")
                .setName("oldName")
                .setEmail("oldEmail")
                .setPassword("oldPassword")
                .setRoles(Sets.newHashSet(User.Role.USER, User.Role.ADMIN));
        when(userRepository.findById("1")).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());
        SafeUserUpdateCommand command = new SafeUserUpdateCommand()
                .setId("1")
                .setName("someName")
                .setEmail("someEmail")
                .setOldPassword("oldPassword")
                .setNewPassword("newPassword");

        User updatedUser = userService.update(command);

        assertThat(updatedUser.getId()).isEqualTo("1");
        assertThat(updatedUser.getName()).isEqualTo("someName");
        assertThat(updatedUser.getEmail()).isEqualTo("someEmail");
        assertThat(updatedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(updatedUser.getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);

        ArgumentCaptor<UnsafeUserUpdateCommand> unsafeCommandCaptor = ArgumentCaptor.forClass(UnsafeUserUpdateCommand.class);
        verify(userService).update(unsafeCommandCaptor.capture());
        assertThat(unsafeCommandCaptor.getValue()).satisfies(unsafeCommand -> {
            assertThat(unsafeCommand.getId()).isEqualTo("1");
            assertThat(unsafeCommand.getName()).isEqualTo("someName");
            assertThat(unsafeCommand.getEmail()).isEqualTo("someEmail");
            assertThat(unsafeCommand.getNewPassword()).isEqualTo("newPassword");
            assertThat(unsafeCommand.getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
        });
    }

    @Test
    public void shouldFailSafeUserUpdateIfUserNotFound() {

        when(userRepository.findById("1")).thenReturn(Optional.empty());
        SafeUserUpdateCommand command = new SafeUserUpdateCommand()
                .setId("1")
                .setName("someName")
                .setEmail("someEmail")
                .setOldPassword("invalidPassword");

        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void shouldFailSafeUserUpdateIfOldPasswordIsInvalid() {

        when(userRepository.findById("1")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        SafeUserUpdateCommand command = new SafeUserUpdateCommand()
                .setId("1")
                .setName("someName")
                .setEmail("someEmail")
                .setOldPassword("invalidPassword");

        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(InvalidPasswordException.class);
    }
}
