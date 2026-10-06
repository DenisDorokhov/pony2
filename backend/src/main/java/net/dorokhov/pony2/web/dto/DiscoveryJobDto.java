package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;

public final class DiscoveryJobDto extends BaseDto<DiscoveryJobDto> {

    private DiscoveryType discoveryType;
    private DiscoveryJob.Status status;
    private String parameter;
    private LogMessageDto logMessage;
    private DiscoveryResultDto discoveryResult;

    public DiscoveryType getDiscoveryType() {
        return discoveryType;
    }

    public DiscoveryJobDto setDiscoveryType(DiscoveryType discoveryType) {
        this.discoveryType = discoveryType;
        return this;
    }

    public DiscoveryJob.Status getStatus() {
        return status;
    }

    public DiscoveryJobDto setStatus(DiscoveryJob.Status status) {
        this.status = status;
        return this;
    }

    @Nullable
    public String getParameter() {
        return parameter;
    }

    public DiscoveryJobDto setParameter(@Nullable String parameter) {
        this.parameter = parameter;
        return this;
    }

    @Nullable
    public LogMessageDto getLogMessage() {
        return logMessage;
    }

    public DiscoveryJobDto setLogMessage(@Nullable LogMessageDto logMessage) {
        this.logMessage = logMessage;
        return this;
    }

    @Nullable
    public DiscoveryResultDto getDiscoveryResult() {
        return discoveryResult;
    }

    public DiscoveryJobDto setDiscoveryResult(@Nullable DiscoveryResultDto discoveryResult) {
        this.discoveryResult = discoveryResult;
        return this;
    }

    public static DiscoveryJobDto of(DiscoveryJob discoveryJob) {
        return new DiscoveryJobDto()
                .setId(discoveryJob.getId())
                .setCreationDate(discoveryJob.getCreationDate())
                .setUpdateDate(discoveryJob.getUpdateDate())
                .setDiscoveryType(discoveryJob.getType())
                .setStatus(discoveryJob.getStatus())
                .setParameter(discoveryJob.getParameter())
                .setLogMessage(discoveryJob.getLogMessage() != null ? LogMessageDto.of(discoveryJob.getLogMessage()) : null)
                .setDiscoveryResult(discoveryJob.getDiscoveryResult() != null ? DiscoveryResultDto.of(discoveryJob.getDiscoveryResult()) : null);
    }
}
