package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public final class DiscoveryTaskPageDto extends PageDto<DiscoveryTaskPageDto> {

    private List<DiscoveryTaskSummaryDto> discoveryTasks;

    public List<DiscoveryTaskSummaryDto> getDiscoveryTasks() {
        return discoveryTasks;
    }

    public DiscoveryTaskPageDto setDiscoveryTasks(List<DiscoveryTaskSummaryDto> discoveryTasks) {
        this.discoveryTasks = discoveryTasks;
        return this;
    }

    public static DiscoveryTaskPageDto of(Page<DiscoveryTask> taskPage, Function<DiscoveryTask, DiscoveryTaskSummaryDto> taskMapper) {
        return new DiscoveryTaskPageDto()
                .setPageIndex(taskPage.getNumber())
                .setPageSize(taskPage.getSize())
                .setTotalPages(taskPage.getTotalPages())
                .setDiscoveryTasks(taskPage.getContent().stream()
                        .map(taskMapper)
                        .toList());
    }
}
