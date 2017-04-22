package net.dorokhov.pony.user.draft;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class CurrentUserUpdateDraft {

    private final String name;
    private final String email;
    private final String oldPassword;
    private final String newPassword;

    private CurrentUserUpdateDraft(Builder builder) {
        checkNotNull(builder.name);
        checkNotNull(builder.email);
        checkNotNull(builder.oldPassword);
        name = builder.name;
        email = builder.email;
        oldPassword = builder.oldPassword;
        newPassword = builder.newPassword;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public Optional<String> getNewPassword() {
        return Optional.ofNullable(newPassword);
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private String name;
        private String email;
        private String oldPassword;
        private String newPassword;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder oldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
            return this;
        }

        public Builder newPassword(String newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        public CurrentUserUpdateDraft build() {
            return new CurrentUserUpdateDraft(this);
        }
    }
}
