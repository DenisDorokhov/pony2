package net.dorokhov.pony2.web.service;

import jakarta.servlet.http.HttpServletRequest;
import net.dorokhov.pony2.core.installation.service.BuildVersionProvider;
import net.dorokhov.pony2.web.dto.opensubsonic.response.OpenSubsonicEmptyResponse;
import net.dorokhov.pony2.web.dto.opensubsonic.response.OpenSubsonicErrorResponse;
import net.dorokhov.pony2.web.dto.opensubsonic.response.OpenSubsonicResponse;
import org.springframework.stereotype.Component;

@Component
public class OpenSubsonicResponseService {

    public static final String PATH_PREFIX = "/opensubsonic";

    public static final int ERROR_GENERIC = 0; // A generic error.
    public static final int ERROR_REQUIRED_PARAMETER_MISSING = 10; // Required parameter is missing.
    public static final int ERROR_INCOMPATIBLE_CLIENT_VERSION = 20; // Incompatible Subsonic REST protocol version. Client must upgrade.
    public static final int ERROR_INCOMPATIBLE_SERVER_VERSION = 30; // Incompatible Subsonic REST protocol version. Server must upgrade.
    public static final int ERROR_WRONG_USERNAME_PASSWORD = 40; // Wrong username or password.
    public static final int ERROR_TOKEN_AUTHENTICATION_NOT_SUPPORTED = 41; // Token authentication not supported for LDAP users.
    public static final int ERROR_AUTHENTICATION_MECHANISM_NOT_SUPPORTED = 42; // Provided authentication mechanism not supported.
    public static final int ERROR_MULTIPLE_AUTHENTICATION_MECHANISMS_PROVIDED = 43; // Multiple conflicting authentication mechanisms provided.
    public static final int ERROR_INVALID_API_KEY = 44; // Invalid API key.
    public static final int ERROR_UNAUTHORIZED = 50; // User is not authorized for the given operation.
    public static final int ERROR_TRIAL_PERIOD_OVER = 60; // The trial period for the Subsonic server is over. Please upgrade to Subsonic Premium. Visit subsonic.org for details.
    public static final int ERROR_NOT_FOUND = 70; // The requested data was not found.

    private static final String TYPE = "pony";
    private static final String VERSION = "1.16.1";

    private final BuildVersionProvider buildVersionProvider;

    public OpenSubsonicResponseService(BuildVersionProvider buildVersionProvider) {
        this.buildVersionProvider = buildVersionProvider;
    }

    public boolean isOpenSubsonicRequest(HttpServletRequest request) {
        return request.getServletPath().startsWith(PATH_PREFIX + "/");
    }

    public OpenSubsonicResponse<OpenSubsonicEmptyResponse> createSuccessful() {
        return createSuccessful(new OpenSubsonicEmptyResponse());
    }

    public <T extends OpenSubsonicResponse.AbstractResponse<T>> OpenSubsonicResponse<T> createSuccessful(T body) {
        return new OpenSubsonicResponse<>(body
                .setStatus("ok")
                .setVersion(VERSION)
                .setType(TYPE)
                .setServerVersion(buildVersionProvider.getBuildVersion().getVersion())
                .setOpenSubsonic(true)
        );
    }

    public OpenSubsonicResponse<OpenSubsonicErrorResponse> createError(int code, String message) {
        return new OpenSubsonicResponse<>(new OpenSubsonicErrorResponse(new OpenSubsonicResponse.AbstractResponse.Error()
                .setCode(code)
                .setMessage(message))
                .setStatus("failed")
                .setVersion(VERSION)
                .setType(TYPE)
                .setServerVersion(buildVersionProvider.getBuildVersion().getVersion())
                .setOpenSubsonic(true)
        );
    }
}
