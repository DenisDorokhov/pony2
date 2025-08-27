package net.dorokhov.pony2.web.common;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.common.RethrowingLambdas;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class StreamingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingUtils.class);

    public static void handleStreamingExceptions(RethrowingLambdas.ThrowingRunnable runnable, HttpServletResponse response) {
        try {
            runnable.run();
        } catch (Exception e) {
            if (e instanceof IOException) {
                if (isConnectionReset(e)) {
                    // Filter out broken pipe exceptions, as they could easily happen during streaming.
                    LOGGER.trace("Broken pipe error occurred.", e);
                } else {
                    LOGGER.error("Unexpected error occurred.", e);
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } catch (Exception ignored) {}
                }
            } else if (e instanceof ObjectNotFoundException) {
                try {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                } catch (IOException ioe) {
                    LOGGER.error("Could not send HTTP status.", ioe);
                }
            } else {
                LOGGER.error("Unexpected error during file distribution.", e);
            }
        }
    }

    public static boolean isConnectionReset(Exception e) {
        Throwable rootCause = e;
        try {
            rootCause = Throwables.getRootCause(e);
        } catch (IllegalArgumentException iae) {
            LOGGER.error("Could not get root cause of exception.", iae);
        }
        if (rootCause instanceof SocketTimeoutException) {
            return true;
        }
        String normalizedError = Strings.nullToEmpty(rootCause.getMessage()).toLowerCase();
        return normalizedError.contains("broken pipe") || normalizedError.contains("connection reset by peer") || normalizedError.contains("connection was aborted");
    }
}
