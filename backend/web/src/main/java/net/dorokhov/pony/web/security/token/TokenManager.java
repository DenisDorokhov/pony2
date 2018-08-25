package net.dorokhov.pony.web.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.primitives.Longs;
import net.dorokhov.pony.web.service.exception.SecretNotFoundException;
import net.dorokhov.pony.web.security.token.exception.InvalidTokenException;
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

    public String createAccessToken(Long userId) {
        return JWT.create()
                .withSubject(userId.toString())
                .sign(buildSignatureAlgorithm());
    }

    public Long verifyAccessToken(String token) throws InvalidTokenException {
        JWTVerifier verifier = JWT.require(buildSignatureAlgorithm()).build();
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
        Long userId = Longs.tryParse(subject);
        if (userId == null) {
            throw new InvalidTokenException();
        }
        return userId;
    }

    private Algorithm buildSignatureAlgorithm() {
        try {
            return Algorithm.HMAC256(tokenSecretManager.fetchTokenSecret());
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize signature algorithm.", e);
        }
    }
}
