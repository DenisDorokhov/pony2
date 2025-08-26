package net.dorokhov.pony2.web.dto.opensubsonic;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenSubsonicResponseDto<T extends OpenSubsonicResponseDto.AbstractResponse<T>> {

    @JsonProperty("subsonic-response")
    private T response;

    public OpenSubsonicResponseDto(T response) {
        this.response = response;
    }

    public T getResponse() {
        return response;
    }

    public OpenSubsonicResponseDto<T> setResponse(T response) {
        this.response = response;
        return this;
    }

    @SuppressWarnings("unchecked")
    public abstract static class AbstractResponse<T extends OpenSubsonicResponseDto.AbstractResponse<T>> {

        private String status;
        private String version;
        private String type;
        private String serverVersion;
        private boolean openSubsonic = true;
        private Error error;

        public String getStatus() {
            return status;
        }

        public T setStatus(String status) {
            this.status = status;
            return (T) this;
        }

        public String getVersion() {
            return version;
        }

        public T setVersion(String version) {
            this.version = version;
            return (T) this;
        }

        public String getType() {
            return type;
        }

        public T setType(String type) {
            this.type = type;
            return (T) this;
        }

        public String getServerVersion() {
            return serverVersion;
        }

        public T setServerVersion(String serverVersion) {
            this.serverVersion = serverVersion;
            return (T) this;
        }

        public boolean isOpenSubsonic() {
            return openSubsonic;
        }

        public T setOpenSubsonic(boolean openSubsonic) {
            this.openSubsonic = openSubsonic;
            return (T) this;
        }

        public Error getError() {
            return error;
        }

        public T setError(Error error) {
            this.error = error;
            return (T) this;
        }

        public static class Error {

            private int code;
            private String message;
            private String helpUrl;

            public int getCode() {
                return code;
            }

            public Error setCode(int code) {
                this.code = code;
                return this;
            }

            public String getMessage() {
                return message;
            }

            public Error setMessage(String message) {
                this.message = message;
                return this;
            }

            public String getHelpUrl() {
                return helpUrl;
            }

            public Error setHelpUrl(String helpUrl) {
                this.helpUrl = helpUrl;
                return this;
            }
        }
    }
}
