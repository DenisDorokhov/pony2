package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.common.JsonConverter;

public final class DiscoveryTaskDto extends BaseDto<DiscoveryTaskDto> {

    private DiscoveryTask.Status status;
    private DiscoveryTaskType type;
    private String result;
    private String rawRequests;
    private String rawResults;

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

    public @Nullable String getRawRequests() {
        return rawRequests;
    }

    public DiscoveryTaskDto setRawRequests(@Nullable String rawRequests) {
        this.rawRequests = rawRequests;
        return this;
    }

    public @Nullable String getRawResults() {
        return rawResults;
    }

    public DiscoveryTaskDto setRawResults(@Nullable String rawResults) {
        this.rawResults = rawResults;
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
                .setRawRequests(task.getRawRequests() != null ? JsonConverter.toJson(task.getRawRequests()) : null)
                .setRawResults(task.getRawResults() != null ? JsonConverter.toJson(task.getRawResults()) : null);
    }
}
