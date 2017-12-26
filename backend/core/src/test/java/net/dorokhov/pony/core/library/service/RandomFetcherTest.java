package net.dorokhov.pony.core.library.service;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RandomFetcherTest {

    @Mock
    private RandomFetcher.Repository<Integer> repository;
    
    private RandomFetcher randomFetcher = new RandomFetcher();
    
    @Test
    public void shouldFetchRandomItems() {

        when(repository.fetchCount()).thenReturn(10L);
        when(repository.fetchContent(any())).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            return ImmutableList.of(pageable.getPageNumber());
        });

        // 100 iterations for 10 items should cover randomness.
        for (int i = 0; i < 100; i++) {

            List<Integer> result = randomFetcher.fetch(10, repository);

            assertThat(result).hasSize(10);
            result.forEach(index -> {
                assertThat(index).isLessThan(10);
                assertThat(index).isGreaterThanOrEqualTo(0);
            });
        }
    }

    @Test
    public void shouldFetchRandomItemsIfTotalCountIsLessThanRequestedCount() {

        when(repository.fetchCount()).thenReturn(5L);
        when(repository.fetchContent(any())).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            return ImmutableList.of(pageable.getPageNumber());
        });

        List<Integer> result = randomFetcher.fetch(1000, repository);

        assertThat(result).hasSize(1000);
        result.forEach(index -> {
            assertThat(index).isLessThan(5);
            assertThat(index).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    public void shouldFetchRandomItemsIfTotalCountChanged() {

        when(repository.fetchCount())
                .thenReturn(1000L)
                .thenReturn(2L);
        when(repository.fetchContent(any()))
                .thenReturn(ImmutableList.of(0))
                .thenReturn(emptyList())
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(0);
                    return ImmutableList.of(pageable.getPageNumber());
                });

        List<Integer> result = randomFetcher.fetch(1000, repository);

        assertThat(result).hasSize(1000);
        result.forEach(index -> {
            assertThat(index).isLessThan(2);
            assertThat(index).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    public void shouldFetchNoItemsIfTotalCountChangedToZero() {

        when(repository.fetchCount())
                .thenReturn(5L)
                .thenReturn(0L);
        when(repository.fetchContent(any()))
                .thenReturn(ImmutableList.of(0))
                .thenReturn(emptyList());

        List<Integer> result = randomFetcher.fetch(10, repository);

        assertThat(result).isEmpty();
    }
}