package net.dorokhov.pony.web.controller.common;

public class ApiResponseValues {
    
    public static final int BAD_REQUEST_CODE = 400;
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int FORBIDDEN_CODE = 403;
    public static final int NOT_FOUND_CODE = 404;
    
    public static final String BAD_REQUEST_MESSAGE = "Invalid request.";
    public static final String UNAUTHORIZED_MESSAGE = "Authentication failed.";
    public static final String FORBIDDEN_MESSAGE = "Access denied.";
    
    private ApiResponseValues() {
    }
}
