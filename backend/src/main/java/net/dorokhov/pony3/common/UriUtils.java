package net.dorokhov.pony3.common;

import java.io.File;

public final class UriUtils {

    /**
     * Performance optimization for file.toURI().getPath() on Windows and Mac.
     */
    public static String fileToUriPath(File file) {
        String path = file.getAbsolutePath();
        if (!path.startsWith("/")) {
            return "/" + path.replaceAll("\\\\", "/");
        }
        return path;
    }
}
