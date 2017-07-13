package net.dorokhov.pony.common;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class PageWalkerTest {

    @Test
    public void shouldWalk() throws Exception {

        Pageable firstPageable = new PageRequest(0, 2);

        List<Page<String>> pages = new ArrayList<>();
        pages.add(new PageImpl<>(ImmutableList.of("1", "2"), firstPageable, 5));
        pages.add(new PageImpl<>(ImmutableList.of("3", "4"), new PageRequest(1, 2), 5));
        pages.add(new PageImpl<>(ImmutableList.of("5"), new PageRequest(2, 1), 5));
        pages.add(new PageImpl<>(emptyList()));

        List<String> processedItems = new ArrayList<>();
        PageWalker.walk(firstPageable, processedItems::add, pageable -> pages.get(pageable.getPageNumber()));
        assertThat(processedItems).containsExactly("1", "2", "3", "4", "5");
    }
}
