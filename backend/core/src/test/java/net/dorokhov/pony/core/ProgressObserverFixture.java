package net.dorokhov.pony.core;

import net.dorokhov.pony.core.library.service.scan.ProgressObserver;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgressObserverFixture implements ProgressObserver {

    private final List<Long> itemsCompleteCalls = new ArrayList<>();
    private final List<Long> itemsTotalCalls = new ArrayList<>();

    @Override
    public void onProgress(long itemsComplete, long itemsTotal) {
        itemsCompleteCalls.add(itemsComplete);
        itemsTotalCalls.add(itemsTotal);
    }

    public int size() {
        return itemsCompleteCalls.size();
    }

    public void assertThatAt(int index, long itemsComplete, long itemsTotal) {
        assertThat(itemsCompleteCalls).element(index).isEqualTo(itemsComplete);
        assertThat(itemsTotalCalls).element(index).isEqualTo(itemsTotal);
    }
}
