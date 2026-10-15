package net.dorokhov.pony2.web.dto;

public final class DiscoveryJobDetailsDto {

    private DiscoveryJobDto discoveryJob;
    private long taskCount;

    public DiscoveryJobDto getDiscoveryJob() {
        return discoveryJob;
    }

    public DiscoveryJobDetailsDto setDiscoveryJob(DiscoveryJobDto discoveryJob) {
        this.discoveryJob = discoveryJob;
        return this;
    }

    public long getTaskCount() {
        return taskCount;
    }

    public DiscoveryJobDetailsDto setTaskCount(long taskCount) {
        this.taskCount = taskCount;
        return this;
    }
}
