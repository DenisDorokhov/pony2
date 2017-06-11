package net.dorokhov.pony.user.service.impl;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.repository.UserRepository;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony.user.service.exception.UserExistsException;
import net.dorokhov.pony.user.service.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

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
    public Page<User> getAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public User create(UserCreationCommand command) throws UserExistsException {
        String email = command.getEmail().trim();
        if (getByEmail(email) != null) {
            throw new UserExistsException(email);
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
    public User update(UnsafeUserUpdateCommand command) throws UserNotFoundException, UserExistsException {
        User userToUpdate = getById(command.getId());
        if (userToUpdate == null) {
            throw new UserNotFoundException(command.getId());
        }
        User sameEmailUser = getByEmail(command.getEmail());
        if (sameEmailUser != null && !Objects.equals(sameEmailUser.getId(), command.getId())) {
            throw new UserExistsException(command.getEmail());
        }
        String password = Optional.ofNullable(command.getNewPassword())
                .map(passwordEncoder::encode)
                .orElse(userToUpdate.getPassword());
        logger.info("Updating user '{}'.", command.getId());
        return userRepository.save(User.builder(userToUpdate)
                .name(command.getName())
                .email(command.getEmail())
                .password(password)
                .roles(command.getRoles())
                .build());
    }

    @Override
    @Transactional
    public User update(SafeUserUpdateCommand command) throws InvalidPasswordException, UserNotFoundException, UserExistsException {
        User user = userRepository.findOne(command.getId());
        if (user == null) {
            throw new UserNotFoundException(command.getId());
        }
        if (!passwordEncoder.matches(command.getOldPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }
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
}
