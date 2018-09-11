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

    public String createAccessTokenForUserId(String userId) {
        return JWT.create()
                .withSubject(userId)
                .sign(buildSignatureAlgorithm());
    }

    public String verifyAccessTokenAndGetUserId(String token) throws InvalidTokenException {
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
        return subject;
    }

    private Algorithm buildSignatureAlgorithm() {
        try {
            return Algorithm.HMAC256(tokenSecretManager.fetchTokenSecret());
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize signature algorithm.", e);
        }
    }
}
