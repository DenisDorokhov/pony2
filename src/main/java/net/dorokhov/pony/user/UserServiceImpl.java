package net.dorokhov.pony.user;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.dorokhov.pony.entity.User;
import net.dorokhov.pony.repository.UserRepository;
import net.dorokhov.pony.user.domain.CurrentUserUpdateDraft;
import net.dorokhov.pony.user.domain.UserCreationDraft;
import net.dorokhov.pony.user.domain.UserToken;
import net.dorokhov.pony.user.domain.UserUpdateDraft;
import net.dorokhov.pony.user.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final Logger log = LoggerFactory.getLogger(getClass());

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

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getById(Long id) {
        return Optional.ofNullable(userRepository.findOne(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getByEmail(String email) {
        return Optional.ofNullable(userRepository.findByEmail(email));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public User create(UserCreationDraft draft) throws UserExistsException {
        String email = draft.getEmail().trim();
        if (getByEmail(email).isPresent()) {
            throw new UserExistsException(email);
        }
        User createdUser = userRepository.save(User.builder()
                .name(draft.getName())
                .email(draft.getEmail())
                .password(passwordEncoder.encode(draft.getPassword()))
                .roles(draft.getRoles())
                .build());
        log.info("Creating user '{}'.", createdUser.getId());
        return createdUser;
    }

    @Override
    @Transactional
    public User update(UserUpdateDraft draft) throws UserNotFoundException, UserExistsException {

        User userToUpdate = getById(draft.getId())
                .orElseThrow(() -> new UserNotFoundException(draft.getId()));
        boolean userExists = getByEmail(draft.getEmail())
                .map(user -> !user.getId().equals(draft.getId()))
                .orElse(false);
        if (userExists) {
            throw new UserExistsException(draft.getEmail());
        }
        String password = draft.getNewPassword().map(passwordEncoder::encode).orElse(userToUpdate.getPassword());

        log.info("Updating user '{}'.", draft.getId());
        User updatedUser = userRepository.save(User.builder(userToUpdate)
                .name(draft.getName())
                .email(draft.getEmail())
                .password(password)
                .roles(draft.getRoles())
                .build());

        getCurrentUser().ifPresent(currentUser -> {
            if (updatedUser.getId().equals(currentUser.getId())) {
                UserDetailsImpl userDetails = new UserDetailsImpl(updatedUser);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()));
            }
        });

        return updatedUser;
    }

    @Override
    @Transactional
    public void delete(Long id) throws UserNotFoundException, DeletingCurrentUserException {
        User userToDelete = userRepository.findOne(id);
        if (userToDelete == null) {
            throw new UserNotFoundException(id);
        }
        getCurrentUser().ifPresent(currentUser -> {
            if (userToDelete.getId().equals(currentUser.getId())) {
                throw new DeletingCurrentUserException(id);
            }
        });
        log.info("Deleting user '{}'.", userToDelete.getId());
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
    public Optional<User> logout() {
        return getCurrentUser().map(user -> {
            log.info("Logging out user '{}'.", user.getId());
            SecurityContextHolder.clearContext();
            return user;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(authentication -> (UserDetailsImpl) authentication.getPrincipal())
                .map(UserDetailsImpl::getUser);
    }

    @Override
    @Transactional
    public User updateCurrentUser(CurrentUserUpdateDraft draft) throws
            NotAuthenticatedException, InvalidPasswordException,
            UserNotFoundException, UserExistsException {
        User currentUser = getCurrentUser().orElseThrow(NotAuthenticatedException::new);
        if (!passwordEncoder.matches(draft.getOldPassword(), currentUser.getPassword())) {
            throw new InvalidPasswordException();
        }
        return update(UserUpdateDraft.builder()
                .id(currentUser.getId())
                .name(draft.getName())
                .email(draft.getEmail())
                .newPassword(draft.getNewPassword().orElse(null))
                .roles(currentUser.getRoles())
                .build());
    }

    private Algorithm buildSignatureAlgorithm() {
        try {
            return Algorithm.HMAC256(tokenSecretManager.getTokenSecret());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
