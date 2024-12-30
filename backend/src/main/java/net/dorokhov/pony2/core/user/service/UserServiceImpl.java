package net.dorokhov.pony2.core.user.service;

import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.domain.UserCreatedEvent;
import net.dorokhov.pony2.api.user.domain.UserDeletingEvent;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.api.user.service.command.SafeUserUpdateCommand;
import net.dorokhov.pony2.api.user.service.command.UnsafeUserUpdateCommand;
import net.dorokhov.pony2.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony2.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony2.api.user.service.exception.InvalidPasswordException;
import net.dorokhov.pony2.api.user.service.exception.UserNotFoundException;
import net.dorokhov.pony2.core.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getById(String id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkUserPassword(String id, String password) throws UserNotFoundException {
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
        if (getByEmail(email).isPresent()) {
            throw new DuplicateEmailException(email);
        }
        User createdUser = userRepository.save(new User()
                .setName(command.getName())
                .setEmail(command.getEmail())
                .setPassword(passwordEncoder.encode(command.getPassword()))
                .setRoles(command.getRoles())
        );
        applicationEventPublisher.publishEvent(new UserCreatedEvent(createdUser.getId()));
        logger.info("Creating user '{}'.", createdUser.getId());
        return createdUser;
    }

    @Override
    @Transactional
    public User update(UnsafeUserUpdateCommand command) throws UserNotFoundException, DuplicateEmailException {
        User userToUpdate = getById(command.getId()).orElse(null);
        if (userToUpdate == null) {
            throw new UserNotFoundException(command.getId());
        }
        User sameEmailUser = getByEmail(command.getEmail()).orElse(null);
        if (sameEmailUser != null && !Objects.equals(sameEmailUser.getId(), command.getId())) {
            throw new DuplicateEmailException(command.getEmail());
        }
        String password = Optional.ofNullable(command.getNewPassword())
                .map(passwordEncoder::encode)
                .orElse(userToUpdate.getPassword());
        logger.info("Updating user '{}'.", command.getId());
        return userRepository.saveAndFlush(userToUpdate
                .setName(command.getName())
                .setEmail(command.getEmail())
                .setPassword(password)
                .setRoles(command.getRoles())
        );
    }

    @Override
    @Transactional
    public User update(SafeUserUpdateCommand command) throws InvalidPasswordException, UserNotFoundException, DuplicateEmailException {
        User user = doCheckUserPassword(command.getId(), command.getOldPassword());
        return update(new UnsafeUserUpdateCommand()
                .setId(user.getId())
                .setName(command.getName())
                .setEmail(command.getEmail())
                .setNewPassword(command.getNewPassword())
                .setRoles(user.getRoles())
        );
    }

    @Override
    @Transactional
    public void delete(String id) throws UserNotFoundException {
        User userToDelete = userRepository.findById(id).orElse(null);
        if (userToDelete == null) {
            throw new UserNotFoundException(id);
        }
        applicationEventPublisher.publishEvent(new UserDeletingEvent(id));
        userRepository.deleteById(id);
        logger.info("Deleting user '{}'.", userToDelete.getId());
    }
    
    private User doCheckUserPassword(String id, String password) throws UserNotFoundException, InvalidPasswordException {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException();
        }
        return user;
    }
}
