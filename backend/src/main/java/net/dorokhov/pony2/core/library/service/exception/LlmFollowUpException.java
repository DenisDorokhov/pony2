package net.dorokhov.pony2.core.library.service.exception;

public class LlmFollowUpException extends RuntimeException {

    private final String prompt;

    public LlmFollowUpException(String prompt, Throwable cause) {
        super(cause.getMessage(), cause);
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
