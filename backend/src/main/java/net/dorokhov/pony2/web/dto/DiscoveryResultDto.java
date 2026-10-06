package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.DiscoveryResult;
import net.dorokhov.pony2.api.library.domain.DiscoveryType;

import java.time.LocalDateTime;

public final class DiscoveryResultDto {

    private String id;
    private LocalDateTime date;
    private DiscoveryType discoveryType;
    private Long completedTasks;
    private Long failedTasks;

    public String getId() {
        return id;
    }

    public DiscoveryResultDto setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public DiscoveryResultDto setDate(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public DiscoveryType getDiscoveryType() {
        return discoveryType;
    }

    public DiscoveryResultDto setDiscoveryType(DiscoveryType discoveryType) {
        this.discoveryType = discoveryType;
        return this;
    }

    public Long getCompletedTasks() {
        return completedTasks;
    }

    public DiscoveryResultDto setCompletedTasks(Long completedTasks) {
        this.completedTasks = completedTasks;
        return this;
    }

    public Long getFailedTasks() {
        return failedTasks;
    }

    public DiscoveryResultDto setFailedTasks(Long failedTasks) {
        this.failedTasks = failedTasks;
        return this;
    }

    public static DiscoveryResultDto of(DiscoveryResult discoveryResult) {
        return new DiscoveryResultDto()
                .setId(discoveryResult.getId())
                .setDate(discoveryResult.getDate())
                .setDiscoveryType(discoveryResult.getType())
                .setCompletedTasks(discoveryResult.getCompletedTasks())
                .setFailedTasks(discoveryResult.getFailedTasks());
    }
}
