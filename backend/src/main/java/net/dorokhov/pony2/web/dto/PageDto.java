package net.dorokhov.pony2.web.dto;

@SuppressWarnings("unchecked")
public abstract class PageDto<T extends PageDto<?>> {

    private int pageIndex;
    private int pageSize;
    private int totalPages;

    public int getPageIndex() {
        return pageIndex;
    }

    public T setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
        return (T) this;
    }

    public int getPageSize() {
        return pageSize;
    }

    public T setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return (T) this;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public T setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        return (T) this;
    }
}
