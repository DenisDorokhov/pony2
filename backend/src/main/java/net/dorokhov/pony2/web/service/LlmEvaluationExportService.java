package net.dorokhov.pony2.web.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.AlbumDiscovery;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import net.dorokhov.pony2.core.library.repository.*;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class LlmEvaluationExportService {

    private static final PageRequest METADATA_PAGE = PageRequest.of(0, 100);
    private static final PageRequest TASK_PAGE = PageRequest.of(0, 8);

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final ArtistDiscoveryRepository artistDiscoveryRepository;
    private final AlbumDiscoveryRepository albumDiscoveryRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final ArtistGenreRepository artistGenreRepository;
    private final ObjectWriter writer;
    private final TransactionTemplate transactionTemplate;

    public LlmEvaluationExportService(
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            ArtistDiscoveryRepository artistDiscoveryRepository,
            AlbumDiscoveryRepository albumDiscoveryRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            ArtistGenreRepository artistGenreRepository,
            PlatformTransactionManager transactionManager,
            JsonMapper jsonMapper
    ) {
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.artistDiscoveryRepository = artistDiscoveryRepository;
        this.albumDiscoveryRepository = albumDiscoveryRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.artistGenreRepository = artistGenreRepository;
        // An interrupted export must remain incomplete, and the servlet owns the output stream.
        writer = jsonMapper.writerWithDefaultPrettyPrinter().withoutFeatures(
                StreamWriteFeature.AUTO_CLOSE_CONTENT, StreamWriteFeature.AUTO_CLOSE_TARGET);
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
                PROPAGATION_REQUIRES_NEW
        );
        transactionDefinition.setReadOnly(true);
        transactionTemplate = new TransactionTemplate(transactionManager, transactionDefinition);
    }

    public void write(OutputStream outputStream, LocalDateTime maximumCreationDate) {
        try (JsonGenerator generator = writer.createGenerator(outputStream)) {
            generator.writeStartObject();
            generator.writeStringProperty("exportedAt", maximumCreationDate.toString());
            generator.flush();
            for (Kind kind : Kind.values()) {
                generator.writeArrayPropertyStart(kind.entityName() + "Discoveries");
                writeDiscoveries(generator, kind, maximumCreationDate);
                generator.writeEndArray();
            }
            generator.writeBooleanProperty("complete", true);
            generator.writeEndObject();
        }
    }

    private void writeDiscoveries(JsonGenerator generator, Kind kind, LocalDateTime maximumCreationDate) {
        String afterEntityId = "";
        while (true) {
            DiscoveryPage page = findDiscoveries(kind, afterEntityId, maximumCreationDate);
            if (page.lastEntityId() == null) {
                return;
            }
            for (DiscoveryRow discovery : page.discoveries()) {
                generator.writeStartObject();
                writeBase(generator, discovery.id(), discovery.creationDate(), discovery.updateDate());
                generator.writeStringProperty("jobId", discovery.jobId());
                generator.writeObjectPropertyStart(kind.entityName());
                writeMetadata(generator, discovery.entity());
                if (kind == Kind.ARTIST) {
                    writeGenres(generator, discovery.entity().id());
                } else {
                    generator.writeName("year");
                    if (discovery.year() != null) {
                        generator.writeNumber(discovery.year());
                    } else {
                        generator.writeNull();
                    }
                    generator.writeStringProperty("artistId", discovery.artistId());
                }
                generator.writeEndObject();
                generator.writeArrayPropertyStart("tasks");
                writeTasks(generator, kind, discovery.id(), maximumCreationDate);
                generator.writeEndArray();
                generator.writeEndObject();
                generator.flush();
            }
            generator.flush();
            afterEntityId = page.lastEntityId();
        }
    }

    private void writeTasks(
            JsonGenerator generator,
            Kind kind,
            String discoveryId,
            LocalDateTime maximumCreationDate
    ) {
        String afterTaskId = "";
        while (true) {
            List<TaskRow> tasks = findTasks(kind, discoveryId, afterTaskId, maximumCreationDate);
            if (tasks.isEmpty()) {
                return;
            }
            for (TaskRow task : tasks) {
                generator.writePOJO(task);
            }
            generator.flush();
            afterTaskId = tasks.getLast().id();
        }
    }

    private void writeGenres(JsonGenerator generator, String artistId) {
        generator.writeArrayPropertyStart("genres");
        String afterGenreId = "";
        while (true) {
            List<Metadata> genres = findGenres(artistId, afterGenreId);
            if (genres.isEmpty()) {
                break;
            }
            for (Metadata genre : genres) {
                generator.writeStartObject();
                writeMetadata(generator, genre);
                generator.writeEndObject();
            }
            generator.flush();
            afterGenreId = genres.getLast().id();
        }
        generator.writeEndArray();
    }

    private void writeMetadata(JsonGenerator generator, Metadata metadata) {
        writeBase(generator, metadata.id(), metadata.creationDate(), metadata.updateDate());
        generator.writeStringProperty("name", metadata.name());
        generator.writeStringProperty("artworkId", metadata.artworkId());
    }

    private DiscoveryPage findDiscoveries(Kind kind, String afterEntityId, LocalDateTime maximumCreationDate) {
        return inReadTransaction(() -> findDiscoveriesInTransaction(kind, afterEntityId, maximumCreationDate));
    }

    private DiscoveryPage findDiscoveriesInTransaction(
            Kind kind,
            String afterEntityId,
            LocalDateTime maximumCreationDate
    ) {
        List<DiscoveryRow> discoveries = new ArrayList<>();
        String lastEntityId = null;
        switch (kind) {
            case ARTIST -> {
                for (Artist artist : artistRepository.findForEvaluation(afterEntityId, METADATA_PAGE)) {
                    lastEntityId = artist.getId();
                    artistDiscoveryRepository.findLatestForEvaluation(artist.getId(), maximumCreationDate, Limit.of(1))
                            .map(this::toRow).ifPresent(discoveries::add);
                }
            }
            case ALBUM -> {
                for (Album album : albumRepository.findForEvaluation(afterEntityId, METADATA_PAGE)) {
                    lastEntityId = album.getId();
                    albumDiscoveryRepository.findLatestForEvaluation(album.getId(), maximumCreationDate, Limit.of(1))
                            .map(this::toRow).ifPresent(discoveries::add);
                }
            }
        }
        return new DiscoveryPage(discoveries, lastEntityId);
    }

    private List<TaskRow> findTasks(
            Kind kind,
            String discoveryId,
            String afterTaskId,
            LocalDateTime maximumCreationDate
    ) {
        return inReadTransaction(() -> findTasksInTransaction(kind, discoveryId, afterTaskId, maximumCreationDate));
    }

    private List<TaskRow> findTasksInTransaction(
            Kind kind,
            String discoveryId,
            String afterTaskId,
            LocalDateTime maximumCreationDate
    ) {
        List<String> taskIds = switch (kind) {
            case ARTIST -> artistDiscoveryRepository.findTaskIdsForEvaluation(
                    discoveryId, afterTaskId, maximumCreationDate, TASK_PAGE
            );
            case ALBUM -> albumDiscoveryRepository.findTaskIdsForEvaluation(
                    discoveryId, afterTaskId, maximumCreationDate, TASK_PAGE
            );
        };
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return discoveryTaskRepository.findForEvaluationByIds(taskIds).stream()
                .map(task -> new TaskRow(
                        task.getId(),
                        task.getCreationDate().toString(),
                        task.getUpdateDate() != null ? task.getUpdateDate().toString() : null,
                        task.getJob().getId(),
                        task.getType().name(),
                        task.getStatus().name(),
                        task.getParameter(),
                        task.getResult(),
                        task.getRawRequest(),
                        task.getRawResult()
                ))
                .toList();
    }

    private List<Metadata> findGenres(String artistId, String afterGenreId) {
        return inReadTransaction(() -> artistGenreRepository
                .findGenresForEvaluation(artistId, afterGenreId, METADATA_PAGE)
                .stream()
                .map(genre -> new Metadata(
                        genre.getId(),
                        genre.getCreationDate(),
                        genre.getUpdateDate(),
                        genre.getName(),
                        genre.getArtwork() != null ? genre.getArtwork().getId() : null
                ))
                .toList());
    }

    private <T> T inReadTransaction(Supplier<T> supplier) {
        return requireNonNull(transactionTemplate.execute(status -> supplier.get()));
    }

    private DiscoveryRow toRow(ArtistDiscovery discovery) {
        Artist artist = discovery.getArtist();
        Metadata metadata = new Metadata(artist.getId(), artist.getCreationDate(), artist.getUpdateDate(),
                artist.getName(), artist.getArtwork() != null ? artist.getArtwork().getId() : null);
        return new DiscoveryRow(discovery.getId(), discovery.getCreationDate(), discovery.getUpdateDate(),
                discovery.getJob().getId(), metadata, null, null);
    }

    private DiscoveryRow toRow(AlbumDiscovery discovery) {
        Album album = discovery.getAlbum();
        Metadata metadata = new Metadata(album.getId(), album.getCreationDate(), album.getUpdateDate(),
                album.getName(), album.getArtwork() != null ? album.getArtwork().getId() : null);
        return new DiscoveryRow(discovery.getId(), discovery.getCreationDate(), discovery.getUpdateDate(),
                discovery.getJob().getId(), metadata, album.getYear(), album.getArtist().getId());
    }

    private void writeBase(
            JsonGenerator generator,
            String id,
            LocalDateTime creationDate,
            @Nullable LocalDateTime updateDate
    ) {
        generator.writeStringProperty("id", id);
        generator.writeStringProperty("creationDate", creationDate.toString());
        generator.writeStringProperty("updateDate", updateDate != null ? updateDate.toString() : null);
    }

    public enum Kind {
        ARTIST("artist"), ALBUM("album");

        private final String entityName;

        Kind(String entityName) {
            this.entityName = entityName;
        }

        public String entityName() {
            return entityName;
        }
    }

    public record Metadata(
            String id,
            LocalDateTime creationDate,
            @Nullable LocalDateTime updateDate,
            @Nullable String name,
            @Nullable String artworkId
    ) {}

    public record DiscoveryRow(
            String id,
            LocalDateTime creationDate,
            @Nullable LocalDateTime updateDate,
            String jobId,
            Metadata entity,
            @Nullable Integer year,
            @Nullable String artistId
    ) {}

    public record DiscoveryPage(List<DiscoveryRow> discoveries, @Nullable String lastEntityId) {}

    public record TaskRow(
            String id,
            String creationDate,
            @Nullable String updateDate,
            String jobId,
            String type,
            String status,
            String parameter,
            @Nullable String result,
            @Nullable String rawRequest,
            @Nullable String rawResult
    ) {}
}
