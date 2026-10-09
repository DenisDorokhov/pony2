package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;

public interface DiscoveryTaskExecution<R> {

    /**
     * Creates and commits the STARTED task and its associations before execution begins.
     */
    DiscoveryTask startTask();

    @Nullable
    R executeTask(DiscoveryTask task);

    /**
     * Runs after COMPLETE is committed. Hook exceptions propagate without changing the task status.
     */
    default void onCompletion(DiscoveryTask task, @Nullable R result) {
    }

    /**
     * Runs after FAILED is committed, except for discovery interruption or a failure to start the task.
     * Hook exceptions propagate without changing the task status.
     */
    default void onError(DiscoveryTask task, RuntimeException error) {
    }
}
