package net.dorokhov.pony.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PageWalker {
    
    private PageWalker() {
    }

    public static <T> void walk(Pageable firstPageable,
                                Consumer<T> itemProcessor,
                                Function<Pageable, Page<T>> nextPageSupplier) {
        Pageable currentPageable = firstPageable;
        Iterator<T> iterator = nextPageSupplier.apply(currentPageable).iterator();
        while (iterator.hasNext()) {
            itemProcessor.accept(iterator.next());
            if (!iterator.hasNext()) {
                currentPageable = currentPageable.next();
                iterator = nextPageSupplier.apply(currentPageable).iterator();
            }
        }
    }
}
