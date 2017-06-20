package net.dorokhov.pony.web.domain;

public abstract class PageDto {

    protected final int pageIndex;
    protected final int pageSize;
    protected final int totalPages;

    protected PageDto(int pageIndex, int pageSize, int totalPages) {
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
