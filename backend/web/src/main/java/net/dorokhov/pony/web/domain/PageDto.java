package net.dorokhov.pony.web.domain;

public abstract class PageDto {

    private final int pageIndex;
    private final int pageSize;
    private final int totalPages;

    PageDto(int pageIndex, int pageSize, int totalPages) {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    public final int getPageIndex() {
        return pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public final int getTotalPages() {
        return totalPages;
    }
}
