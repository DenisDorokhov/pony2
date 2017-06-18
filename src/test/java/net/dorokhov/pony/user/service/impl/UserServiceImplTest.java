package net.dorokhov.pony.user.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.repository.UserRepository;
import net.dorokhov.pony.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static net.dorokhov.pony.fixture.UserFixtures.user;
import static net.dorokhov.pony.fixture.UserFixtures.userBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {
    
    @InjectMocks
    @Spy
    private UserServiceImpl userService;
    
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    public void shouldGetById() throws Exception {
        User user = user();
        when(userRepository.findOne(1L)).thenReturn(user);
        assertThat(userService.getById(1L)).isSameAs(user);
    }

    @Test
    public void shouldGetByEmail() throws Exception {
        User user = user();
        when(userRepository.findByEmail("someEmail")).thenReturn(user);
        assertThat(userService.getByEmail("someEmail")).isSameAs(user);
    }

    @Test
    public void shouldGetAll() throws Exception {
        List<User> users = ImmutableList.of(user());
        when(userRepository.findAll((Sort) any())).thenReturn(users);
        assertThat(userService.getAll()).isSameAs(users);
    }

    @Test
    public void shouldCheckUserPassword() throws Exception {
        User existingUser = user();
        when(userRepository.findOne(existingUser.getId())).thenReturn(existingUser);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        assertThat(userService.checkUserPassword(existingUser.getId(), "somePassword")).isTrue();
    }

    @Test
    public void shouldFailUserPasswordCheckIfUserNotFound() throws Exception {
        when(userRepository.findOne(1L)).thenReturn(null);
        assertThatThrownBy(() -> userService.checkUserPassword(1L, "somePassword")).isInstanceOfSatisfying(UserNotFoundException.class, e -> 
                assertThat(e.getId()).isEqualTo(1L));
    }

    @Test
    public void shouldFailUserPasswordCheckIfPasswordDoesNotMatch() throws Exception {
        User existingUser = user();
        when(userRepository.findOne(existingUser.getId())).thenReturn(existingUser);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        assertThat(userService.checkUserPassword(1L, "somePassword")).isFalse();
    }

    @Test
    public void shouldCreateUser() throws Exception {
        
        when(passwordEncoder.encode("somePassword")).thenReturn("encodedPassword");
        when(userRepository.save((User) any())).thenAnswer(returnsFirstArg());

        UserCreationCommand command = UserCreationCommand.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .roles(User.Role.USER, User.Role.ADMIN)
                .build();
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
    public void shouldFailUserCreationIfUserExists() throws Exception {
        User existingUser = user();
        when(userRepository.findByEmail("someEmail")).thenReturn(existingUser);
        assertThatThrownBy(() -> userService.create(UserCreationCommand.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .build())).isInstanceOf(DuplicateEmailException.class);        
    }

    @Test
    public void shouldUpdateUserUnsafely() throws Exception {

        User existingUser = user();
        when(userRepository.findOne(1L)).thenReturn(existingUser);
        when(userRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());

        UnsafeUserUpdateCommand command = UnsafeUserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .roles(User.Role.USER, User.Role.ADMIN)
                .build();
        User updatedUser = userService.update(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue()).satisfies(user -> {
            assertThat(updatedUser).isSameAs(user);
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getName()).isEqualTo("someName");
            assertThat(user.getEmail()).isEqualTo("someEmail");
            assertThat(user.getPassword()).isEqualTo("somePassword");
            assertThat(user.getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
        });
    }

    @Test
    public void shouldUpdateUserPasswordUnsafely() throws Exception {

        User existingUser = user();
        when(userRepository.findOne(1L)).thenReturn(existingUser);
        when(userRepository.findByEmail(existingUser.getEmail())).thenReturn(existingUser);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());

        UnsafeUserUpdateCommand command = UnsafeUserUpdateCommand.builder()
                .id(existingUser.getId())
                .name(existingUser.getName())
                .email(existingUser.getEmail())
                .newPassword("newPassword")
                .build();
        User updatedUser = userService.update(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue()).satisfies(user -> {
            assertThat(updatedUser).isSameAs(user);
            assertThat(user.getPassword()).isEqualTo("encodedPassword");
        });
    }

    @Test
    public void shouldFailUnsafeUserUpdateIfUserNotFound() throws Exception {
        when(userRepository.findOne(1L)).thenReturn(null);
        UnsafeUserUpdateCommand command = UnsafeUserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .newPassword("somePassword")
                .build();
        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void shouldFailUnsafeUserUpdateIfNewUserEmailExists() throws Exception {
        User userToUpdate = userBuilder().email("otherEmail").build();
        when(userRepository.findOne(1L)).thenReturn(userToUpdate);
        User existingUser = userBuilder().id(2L).build();
        when(userRepository.findByEmail("someEmail")).thenReturn(existingUser);
        UnsafeUserUpdateCommand command = UnsafeUserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .newPassword("somePassword")
                .build();
        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    public void shouldDeleteUser() throws Exception {
        User existingUser = user();
        when(userRepository.findOne(1L)).thenReturn(existingUser);
        userService.delete(1L);
        verify(userRepository).delete(1L);
    }

    @Test
    public void shouldFailUserDeletionIdUserNotFound() throws Exception {
        when(userRepository.findOne(1L)).thenReturn(null);
        assertThatThrownBy(() -> userService.delete(1L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void shouldUpdateUserSafely() throws Exception {

        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");

        User existingUser = User.builder()
                .id(1L)
                .name("oldName")
                .email("oldEmail")
                .password("oldPassword")
                .addRoles(User.Role.USER, User.Role.ADMIN)
                .build();
        when(userRepository.findOne(1L)).thenReturn(existingUser);
        when(userRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());

        SafeUserUpdateCommand command = SafeUserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();
        User updatedUser = userService.update(command);
        
        assertThat(updatedUser.getId()).isEqualTo(1L);
        assertThat(updatedUser.getName()).isEqualTo("someName");
        assertThat(updatedUser.getEmail()).isEqualTo("someEmail");
        assertThat(updatedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(updatedUser.getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);

        ArgumentCaptor<UnsafeUserUpdateCommand> unsafeCommandCaptor = ArgumentCaptor.forClass(UnsafeUserUpdateCommand.class);
        verify(userService).update(unsafeCommandCaptor.capture());
        assertThat(unsafeCommandCaptor.getValue()).satisfies(unsafeCommand -> {
            assertThat(unsafeCommand.getId()).isEqualTo(1L);
            assertThat(unsafeCommand.getName()).isEqualTo("someName");
            assertThat(unsafeCommand.getEmail()).isEqualTo("someEmail");
            assertThat(unsafeCommand.getNewPassword()).isEqualTo("newPassword");
            assertThat(unsafeCommand.getRoles()).containsOnly(User.Role.USER, User.Role.ADMIN);
        });
    }

    @Test
    public void shouldFailSafeUserUpdateIfUserNotFound() throws Exception {
        when(userRepository.findOne(1L)).thenReturn(null);
        SafeUserUpdateCommand command = SafeUserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .oldPassword("invalidPassword")
                .build();
        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    public void shouldFailSafeUserUpdateIfOldPasswordIsInvalid() throws Exception {
        when(userRepository.findOne(1L)).thenReturn(user());
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        SafeUserUpdateCommand command = SafeUserUpdateCommand.builder()
                .id(1L)
                .name("someName")
                .email("someEmail")
                .oldPassword("invalidPassword")
                .build();
        assertThatThrownBy(() -> userService.update(command)).isInstanceOf(InvalidPasswordException.class);
    }
}
