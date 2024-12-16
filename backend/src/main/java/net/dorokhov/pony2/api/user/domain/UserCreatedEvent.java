package net.dorokhov.pony2.api.user.domain;

import org.springframework.context.ApplicationEvent;

public class UserCreatedEvent extends ApplicationEvent {

    public UserCreatedEvent(String userId) {
        super(userId);
    }

    public String getUserId() {
        return (String) getSource();
    }
}
