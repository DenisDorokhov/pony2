package net.dorokhov.pony.web.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.dorokhov.pony.web.security.token.exception.InvalidTokenException;
import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class TokenService {

    private final TokenKeyService tokenKeyService;

    public TokenService(TokenKeyService tokenKeyService) {
        this.tokenKeyService = tokenKeyService;
    }

    @PostConstruct
    public void assureTokenKeysExist() throws IOException {
        try {
            tokenKeyService.fetchAccessTokenKey();
        } catch (SecretNotFoundException e) {
            tokenKeyService.generateAndStoreAccessTokenKey();
        }
        try {
            tokenKeyService.fetchStaticTokenKey();
        } catch (SecretNotFoundException e) {
            tokenKeyService.generateAndStoreStaticTokenKey();
        }
    }
    
    public String generateAccessTokenForUserId(String userId) {
        try {
            return generateJwtTokenForUserId(userId, tokenKeyService.fetchAccessTokenKey());
        } catch (SecretNotFoundException | IOException e) {
            throw new IllegalStateException("Could not generate access token. Does the key file exist?");
        }
    }
    
    public String verifyAccessTokenAndGetUserId(String userId) throws InvalidTokenException {
        try {
            return verifyJwtTokenAndGetUserId(userId, tokenKeyService.fetchAccessTokenKey());
        } catch (SecretNotFoundException | IOException e) {
            throw new IllegalStateException("Could not verify access token. Does the key file exist?");
        }
    }
    
    public String generateStaticTokenForUserId(String userId) {
        try {
            return generateJwtTokenForUserId(userId, tokenKeyService.fetchStaticTokenKey());
        } catch (SecretNotFoundException | IOException e) {
            throw new IllegalStateException("Could not generate static token. Does the key file exist?");
        }
    }
    
    public String verifyStaticTokenAndGetUserId(String userId) throws InvalidTokenException {
        try {
            return verifyJwtTokenAndGetUserId(userId, tokenKeyService.fetchStaticTokenKey());
        } catch (SecretNotFoundException | IOException e) {
            throw new IllegalStateException("Could not verify static token. Does the key file exist?");
        }
    }

    private String generateJwtTokenForUserId(String userId, byte[] key) {
        return JWT.create()
                .withSubject(userId)
                .sign(buildSignatureAlgorithm(key));
    }

    private String verifyJwtTokenAndGetUserId(String token, byte[] key) throws InvalidTokenException {
        JWTVerifier verifier = JWT.require(buildSignatureAlgorithm(key)).build();
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException();
        }
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new InvalidTokenException();
        }
        return subject;
    }

    private Algorithm buildSignatureAlgorithm(byte[] key) {
        try {
            return Algorithm.HMAC256(key);
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize signature algorithm.", e);
        }
    }
}
