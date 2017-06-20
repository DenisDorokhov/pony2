package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.library.domain.ScanJob;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public final class ScanJobPageDto extends PageDto {

    private final List<ScanJobDto> scanJobs;

    ScanJobPageDto(int pageIndex, int pageSize, int totalPages, List<ScanJobDto> scanJobs) {
        super(pageIndex, pageSize, totalPages);
        this.scanJobs = unmodifiableList(scanJobs);
    }

    public List<ScanJobDto> getScanJobs() {
        return scanJobs;
    }

    public static ScanJobPageDto of(Page<ScanJob> scanJobPage) {
        return new ScanJobPageDto(scanJobPage.getNumber(), scanJobPage.getSize(), scanJobPage.getTotalPages(),
                scanJobPage.getContent().stream()
                        .map(ScanJobDto::of)
                        .collect(Collectors.toList()));
    }
}
