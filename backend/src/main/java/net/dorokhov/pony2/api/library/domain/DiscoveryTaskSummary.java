package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record DiscoveryTaskSummary(
        String id,
        LocalDateTime creationDate,
        @Nullable LocalDateTime updateDate,
        DiscoveryTask.Status status,
        DiscoveryTaskType type,
        boolean noResult,
        @Nullable Genre genre,
        @Nullable Artist artist,
        @Nullable Album album,
        @Nullable Song song
) {
    public static DiscoveryTaskSummary of(DiscoveryTask task) {
        return new DiscoveryTaskSummary(task.getId(), task.getCreationDate(), task.getUpdateDate(), task.getStatus(),
                task.getType(), task.getStatus() == DiscoveryTask.Status.COMPLETE && task.getResult() == null,
                task.getGenre(), task.getArtist(), task.getAlbum(), task.getSong());
    }
}
