package net.dorokhov.pony.api.user.service.command;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SafeUserUpdateCommand {

    private final Long id;
    private final String name;
    private final String email;
    private final String oldPassword;
    private final String newPassword;

    private SafeUserUpdateCommand(Builder builder) {
        id = checkNotNull(builder.id);
        name = checkNotNull(builder.name);
        email = checkNotNull(builder.email);
        oldPassword = checkNotNull(builder.oldPassword);
        newPassword = builder.newPassword;
    }

    public Long getId() {
        return id;
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

    @Nullable
    public String getNewPassword() {
        return newPassword;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private Long id;
        private String name;
        private String email;
        private String oldPassword;
        private String newPassword;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        
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

        public Builder newPassword(@Nullable String newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        public SafeUserUpdateCommand build() {
            return new SafeUserUpdateCommand(this);
        }
    }
}
