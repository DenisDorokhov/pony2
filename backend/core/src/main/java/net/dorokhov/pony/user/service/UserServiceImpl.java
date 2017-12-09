package net.dorokhov.pony.user.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.repository.UserRepository;
import net.dorokhov.pony.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public User getById(Long id) {
        return userRepository.findOne(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public User getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAll() {
        return userRepository.findAll(new Sort("name", "email"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkUserPassword(Long id, String password) throws UserNotFoundException {
        try {
            doCheckUserPassword(id, password);
        } catch (InvalidPasswordException e) {
            return false;
        }
        return true;
    }

    @Override
    @Transactional
    public User create(UserCreationCommand command) throws DuplicateEmailException {
        String email = command.getEmail().trim();
        if (getByEmail(email) != null) {
            throw new DuplicateEmailException(email);
        }
        User createdUser = userRepository.save(User.builder()
                .name(command.getName())
                .email(command.getEmail())
                .password(passwordEncoder.encode(command.getPassword()))
                .roles(command.getRoles())
                .build());
        logger.info("Creating user '{}'.", createdUser.getId());
        return createdUser;
    }

    @Override
    @Transactional
    public User update(UnsafeUserUpdateCommand command) throws UserNotFoundException, DuplicateEmailException {
        User userToUpdate = getById(command.getId());
        if (userToUpdate == null) {
            throw new UserNotFoundException(command.getId());
        }
        User sameEmailUser = getByEmail(command.getEmail());
        if (sameEmailUser != null && !Objects.equals(sameEmailUser.getId(), command.getId())) {
            throw new DuplicateEmailException(command.getEmail());
        }
        String password = Optional.ofNullable(command.getNewPassword())
                .map(passwordEncoder::encode)
                .orElse(userToUpdate.getPassword());
        logger.info("Updating user '{}'.", command.getId());
        return userRepository.saveAndFlush(User.builder(userToUpdate)
                .name(command.getName())
                .email(command.getEmail())
                .password(password)
                .roles(command.getRoles())
                .build());
    }

    @Override
    @Transactional
    public User update(SafeUserUpdateCommand command) throws InvalidPasswordException, UserNotFoundException, DuplicateEmailException {
        User user = doCheckUserPassword(command.getId(), command.getOldPassword());
        return update(UnsafeUserUpdateCommand.builder()
                .id(user.getId())
                .name(command.getName())
                .email(command.getEmail())
                .newPassword(command.getNewPassword())
                .roles(user.getRoles())
                .build());
    }

    @Override
    @Transactional
    public void delete(Long id) throws UserNotFoundException {
        User userToDelete = userRepository.findOne(id);
        if (userToDelete == null) {
            throw new UserNotFoundException(id);
        }
        logger.info("Deleting user '{}'.", userToDelete.getId());
        userRepository.delete(id);
    }
    
    private User doCheckUserPassword(Long id, String password) throws UserNotFoundException, InvalidPasswordException {
        User user = userRepository.findOne(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException();
        }
        return user;
    }
}
