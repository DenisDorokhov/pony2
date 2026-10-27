package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;

public final class DiscoveryTaskDto extends BaseDto<DiscoveryTaskDto> {

    private DiscoveryTask.Status status;
    private DiscoveryTaskType type;
    private String result;
    private String rawRequest;
    private String rawResult;

    public DiscoveryTask.Status getStatus() {
        return status;
    }

    public DiscoveryTaskDto setStatus(DiscoveryTask.Status status) {
        this.status = status;
        return this;
    }

    public DiscoveryTaskType getType() {
        return type;
    }

    public DiscoveryTaskDto setType(DiscoveryTaskType type) {
        this.type = type;
        return this;
    }

    public @Nullable String getResult() {
        return result;
    }

    public DiscoveryTaskDto setResult(@Nullable String result) {
        this.result = result;
        return this;
    }

    public @Nullable String getRawRequest() {
        return rawRequest;
    }

    public DiscoveryTaskDto setRawRequest(@Nullable String rawRequest) {
        this.rawRequest = rawRequest;
        return this;
    }

    public @Nullable String getRawResult() {
        return rawResult;
    }

    public DiscoveryTaskDto setRawResult(@Nullable String rawResult) {
        this.rawResult = rawResult;
        return this;
    }

    public static DiscoveryTaskDto of(DiscoveryTask task) {
        return new DiscoveryTaskDto()
                .setId(task.getId())
                .setCreationDate(task.getCreationDate())
                .setUpdateDate(task.getUpdateDate())
                .setStatus(task.getStatus())
                .setType(task.getType())
                .setResult(task.getResult())
                .setRawRequest(task.getRawRequest())
                .setRawResult(task.getRawResult());
    }
}
