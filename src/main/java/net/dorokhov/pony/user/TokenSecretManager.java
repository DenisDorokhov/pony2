package net.dorokhov.pony.user;

import net.dorokhov.pony.user.exception.TokenSecretNotFoundException;

import java.io.IOException;

public interface TokenSecretManager {
    
    String generateAndStoreTokenSecret() throws IOException;
    
    String getTokenSecret() throws TokenSecretNotFoundException;
}
