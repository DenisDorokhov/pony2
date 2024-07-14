package net.dorokhov.pony3.web.dto;

import net.dorokhov.pony3.api.library.domain.ScanJob;
import org.springframework.data.domain.Page;

import java.util.List;

public final class ScanJobPageDto extends PageDto<ScanJobPageDto> {

    private List<ScanJobDto> scanJobs;

    public List<ScanJobDto> getScanJobs() {
        return scanJobs;
    }

    public ScanJobPageDto setScanJobs(List<ScanJobDto> scanJobs) {
        this.scanJobs = scanJobs;
        return this;
    }

    public static ScanJobPageDto of(Page<ScanJob> scanJobPage) {
        return new ScanJobPageDto()
                .setPageIndex(scanJobPage.getNumber())
                .setPageSize(scanJobPage.getSize())
                .setTotalPages(scanJobPage.getTotalPages())
                .setScanJobs(scanJobPage.getContent().stream()
                        .map(ScanJobDto::of)
                        .toList());
    }
}
