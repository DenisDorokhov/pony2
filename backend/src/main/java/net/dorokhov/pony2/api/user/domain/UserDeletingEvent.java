package net.dorokhov.pony2.api.user.domain;

import org.springframework.context.ApplicationEvent;

public class UserDeletingEvent extends ApplicationEvent {

    public UserDeletingEvent(String userId) {
        super(userId);
    }

    public String getUserId() {
        return (String) getSource();
    }
}
