package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public final class DiscoveryJobPageDto extends PageDto<DiscoveryJobPageDto> {

    private List<DiscoveryJobDto> discoveryJobs;

    public List<DiscoveryJobDto> getDiscoveryJobs() {
        return discoveryJobs;
    }

    public DiscoveryJobPageDto setDiscoveryJobs(List<DiscoveryJobDto> discoveryJobs) {
        this.discoveryJobs = discoveryJobs;
        return this;
    }

    public static DiscoveryJobPageDto of(Page<DiscoveryJob> discoveryJobPage, Function<DiscoveryJob, DiscoveryJobDto> mapper) {
        return new DiscoveryJobPageDto()
                .setPageIndex(discoveryJobPage.getNumber())
                .setPageSize(discoveryJobPage.getSize())
                .setTotalPages(discoveryJobPage.getTotalPages())
                .setDiscoveryJobs(discoveryJobPage.getContent().stream()
                        .map(mapper)
                        .toList());
    }
}
