package net.dorokhov.pony.user.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.dorokhov.pony.common.SecretNotFoundException;
import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.domain.UserToken;
import net.dorokhov.pony.user.repository.UserRepository;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.command.CurrentUserUpdateCommand;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.service.command.UserUpdateCommand;
import net.dorokhov.pony.user.service.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserRepository userRepository;
    private final TokenSecretManager tokenSecretManager;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UserServiceImpl(UserRepository userRepository, TokenSecretManager tokenSecretManager,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.tokenSecretManager = tokenSecretManager;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @PostConstruct
    public void assureTokenSecretExists() throws IOException {
        try {
            tokenSecretManager.getTokenSecret();
        } catch (SecretNotFoundException e) {
            tokenSecretManager.generateAndStoreTokenSecret();
        }
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
    public User update(UserUpdateCommand command) throws UserNotFoundException, UserExistsException {

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
        User updatedUser = userRepository.save(User.builder(userToUpdate)
                .name(command.getName())
                .email(command.getEmail())
                .password(password)
                .roles(command.getRoles())
                .build());

        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.equals(updatedUser)) {
            UserDetailsImpl userDetails = new UserDetailsImpl(updatedUser);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));
        }

        return updatedUser;
    }

    @Override
    @Transactional
    public void delete(Long id) throws UserNotFoundException, DeletingCurrentUserException {

        User userToDelete = userRepository.findOne(id);
        if (userToDelete == null) {
            throw new UserNotFoundException(id);
        }
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.equals(userToDelete)) {
            throw new DeletingCurrentUserException(id);
        }

        logger.info("Deleting user '{}'.", userToDelete.getId());
        userRepository.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserToken authenticate(String email, String password) throws InvalidCredentialsException {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException(email);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User currentUser = ((UserDetailsImpl) authentication.getPrincipal()).getUser();
        return new UserToken(currentUser, JWT.create()
                .withSubject(currentUser.getId().toString())
                .sign(buildSignatureAlgorithm()));
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(String token) throws InvalidTokenException {
        JWTVerifier verifier = JWT.require(buildSignatureAlgorithm()).build();
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException();
        }
        User currentUser = userRepository.findOne(Long.valueOf(jwt.getSubject()));
        if (currentUser == null) {
            throw new InvalidTokenException();
        }
        UserDetailsImpl userDetails = new UserDetailsImpl(currentUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public User logout() {
        User user = getCurrentUser();
        if (user != null) {
            logger.info("Logging out user '{}'.", user.getId());
            SecurityContextHolder.clearContext();
            return user;
        } else {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public User getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken))
                .map(authentication -> (UserDetailsImpl) authentication.getPrincipal())
                .map(UserDetailsImpl::getUser)
                .orElse(null);
    }

    @Override
    @Transactional
    public User updateCurrentUser(CurrentUserUpdateCommand command) throws
            NotAuthenticatedException, InvalidPasswordException,
            UserNotFoundException, UserExistsException {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new NotAuthenticatedException();
        }
        if (!passwordEncoder.matches(command.getOldPassword(), currentUser.getPassword())) {
            throw new InvalidPasswordException();
        }

        return update(UserUpdateCommand.builder()
                .id(currentUser.getId())
                .name(command.getName())
                .email(command.getEmail())
                .newPassword(command.getNewPassword())
                .roles(currentUser.getRoles())
                .build());
    }

    private Algorithm buildSignatureAlgorithm() {
        try {
            return Algorithm.HMAC256(tokenSecretManager.getTokenSecret());
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize signature algorithm.", e);
        }
    }
}
