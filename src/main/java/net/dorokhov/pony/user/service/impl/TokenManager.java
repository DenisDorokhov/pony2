package net.dorokhov.pony.user.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.dorokhov.pony.common.SecretNotFoundException;
import net.dorokhov.pony.user.service.exception.InvalidTokenException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class TokenManager {

    private final TokenSecretManager tokenSecretManager;

    public TokenManager(TokenSecretManager tokenSecretManager) {
        this.tokenSecretManager = tokenSecretManager;
    }

    @PostConstruct
    public void assureTokenSecretExists() throws IOException {
        try {
            tokenSecretManager.fetchTokenSecret();
        } catch (SecretNotFoundException e) {
            tokenSecretManager.generateAndStoreTokenSecret();
        }
    }

    public String signToken(String subject) {
        return JWT.create()
                .withSubject(subject)
                .sign(buildSignatureAlgorithm());
    }

    public String verifyToken(String token) throws InvalidTokenException {
        JWTVerifier verifier = JWT.require(buildSignatureAlgorithm()).build();
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException();
        }
        return jwt.getSubject();
    }

    private Algorithm buildSignatureAlgorithm() {
        try {
            return Algorithm.HMAC256(tokenSecretManager.fetchTokenSecret());
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize signature algorithm.", e);
        }
    }
}
