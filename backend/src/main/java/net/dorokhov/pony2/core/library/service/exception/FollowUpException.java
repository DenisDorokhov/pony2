package net.dorokhov.pony2.core.library.service.exception;

public class FollowUpException extends RuntimeException {

    private final String prompt;

    public FollowUpException(String prompt, Throwable cause) {
        super(cause.getMessage(), cause);
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
