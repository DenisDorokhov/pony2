package net.dorokhov.pony2.web.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import net.dorokhov.pony2.web.security.token.exception.InvalidTokenException;
import net.dorokhov.pony2.web.service.exception.SecretNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class TokenService {

    private final TokenKeyService tokenKeyService;

    public TokenService(TokenKeyService tokenKeyService) {
        this.tokenKeyService = tokenKeyService;
    }

    @PostConstruct
    public void assureTokenKeysExist() {
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
        try {
            tokenKeyService.fetchOpenSubsonicKey();
        } catch (SecretNotFoundException e) {
            tokenKeyService.generateAndStoreOpenSubsonicKey();
        }
    }
    
    public String generateAccessTokenForUserId(String userId) {
        try {
            return generateJwtTokenForUserId(userId, tokenKeyService.fetchAccessTokenKey());
        } catch (SecretNotFoundException e) {
            throw new IllegalStateException("Could not generate access token. Does the key file exist?");
        }
    }
    
    public String verifyAccessTokenAndGetUserId(String userId) throws InvalidTokenException {
        try {
            return verifyJwtTokenAndGetUserId(userId, tokenKeyService.fetchAccessTokenKey());
        } catch (SecretNotFoundException e) {
            throw new IllegalStateException("Could not verify access token. Does the key file exist?");
        }
    }
    
    public String generateStaticTokenForUserId(String userId) {
        try {
            return generateJwtTokenForUserId(userId, tokenKeyService.fetchStaticTokenKey());
        } catch (SecretNotFoundException e) {
            throw new IllegalStateException("Could not generate static token. Does the key file exist?");
        }
    }
    
    public String verifyStaticTokenAndGetUserId(String userId) throws InvalidTokenException {
        try {
            return verifyJwtTokenAndGetUserId(userId, tokenKeyService.fetchStaticTokenKey());
        } catch (SecretNotFoundException e) {
            throw new IllegalStateException("Could not verify static token. Does the key file exist?");
        }
    }

    public String generateOpenSubsonicApiKeyForUserId(String userId) {
        try {
            return generateJwtTokenForUserId(userId, tokenKeyService.fetchOpenSubsonicKey());
        } catch (SecretNotFoundException e) {
            throw new IllegalStateException("Could not generate OpenSubsonic API key. Does the key file exist?");
        }
    }

    public String verifyOpenSubsonicApiKeyAndGetUserId(String userId) throws InvalidTokenException {
        try {
            return verifyJwtTokenAndGetUserId(userId, tokenKeyService.fetchOpenSubsonicKey());
        } catch (SecretNotFoundException e) {
            throw new IllegalStateException("Could not verify OpenSubsonic API key. Does the key file exist?");
        }
    }

    private String generateJwtTokenForUserId(String userId, byte[] key) {
        return JWT.create()
                .withSubject(userId)
                .withIssuedAt(new Date())
                .withJWTId(UUID.randomUUID().toString())
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
