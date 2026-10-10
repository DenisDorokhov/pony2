package net.dorokhov.pony2.web.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.AlbumDiscovery;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.Genre;
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

    private static final PageRequest ENTITY_PAGE = PageRequest.of(0, 100);
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

            writeArtistDiscoveries(generator, maximumCreationDate);
            writeAlbumDiscoveries(generator, maximumCreationDate);

            generator.writeEndObject();
        }
    }

    private void writeArtistDiscoveries(JsonGenerator generator, LocalDateTime maximumCreationDate) {
        generator.writeArrayPropertyStart("artistDiscoveries");
        String afterArtistId = "";
        while (true) {
            ArtistDiscoveryPageDto page = findArtistDiscoveryPage(afterArtistId, maximumCreationDate);
            if (page.lastArtistId() == null) {
                break;
            }
            for (ArtistDiscoveryExportDto discovery : page.discoveries()) {
                writeArtistDiscovery(generator, discovery, maximumCreationDate);
                generator.flush();
            }
            generator.flush();
            afterArtistId = page.lastArtistId();
        }
        generator.writeEndArray();
    }

    private void writeAlbumDiscoveries(JsonGenerator generator, LocalDateTime maximumCreationDate) {
        generator.writeArrayPropertyStart("albumDiscoveries");
        String afterAlbumId = "";
        while (true) {
            AlbumDiscoveryPageDto page = findAlbumDiscoveryPage(afterAlbumId, maximumCreationDate);
            if (page.lastAlbumId() == null) {
                break;
            }
            for (AlbumDiscoveryExportDto discovery : page.discoveries()) {
                writeAlbumDiscovery(generator, discovery, maximumCreationDate);
                generator.flush();
            }
            generator.flush();
            afterAlbumId = page.lastAlbumId();
        }
        generator.writeEndArray();
    }

    private void writeArtistDiscovery(
            JsonGenerator generator,
            ArtistDiscoveryExportDto discovery,
            LocalDateTime maximumCreationDate
    ) {
        generator.writeStartObject();
        generator.writeStringProperty("id", discovery.id());
        generator.writeStringProperty("creationDate", discovery.creationDate());
        generator.writeStringProperty("updateDate", discovery.updateDate());
        generator.writeStringProperty("jobId", discovery.jobId());

        generator.writeName("artist");
        writeArtist(generator, discovery.artist());
        writeArtistTasks(generator, discovery.id(), maximumCreationDate);

        generator.writeEndObject();
    }

    private void writeAlbumDiscovery(
            JsonGenerator generator,
            AlbumDiscoveryExportDto discovery,
            LocalDateTime maximumCreationDate
    ) {
        generator.writeStartObject();
        generator.writeStringProperty("id", discovery.id());
        generator.writeStringProperty("creationDate", discovery.creationDate());
        generator.writeStringProperty("updateDate", discovery.updateDate());
        generator.writeStringProperty("jobId", discovery.jobId());

        generator.writeName("album");
        writeAlbum(generator, discovery.album());
        writeAlbumTasks(generator, discovery.id(), maximumCreationDate);

        generator.writeEndObject();
    }

    private void writeArtist(JsonGenerator generator, ArtistExportDto artist) {
        generator.writeStartObject();
        generator.writeStringProperty("id", artist.id());
        generator.writeStringProperty("creationDate", artist.creationDate());
        generator.writeStringProperty("updateDate", artist.updateDate());
        generator.writeStringProperty("name", artist.name());
        generator.writeStringProperty("artworkId", artist.artworkId());
        writeArtistGenres(generator, artist.id());
        generator.writeEndObject();
    }

    private void writeAlbum(JsonGenerator generator, AlbumExportDto album) {
        generator.writeStartObject();
        generator.writeStringProperty("id", album.id());
        generator.writeStringProperty("creationDate", album.creationDate());
        generator.writeStringProperty("updateDate", album.updateDate());
        generator.writeStringProperty("name", album.name());
        generator.writeStringProperty("artworkId", album.artworkId());
        if (album.year() != null) {
            generator.writeNumberProperty("year", album.year());
        } else {
            generator.writeNullProperty("year");
        }
        generator.writeName("artist");
        writeArtist(generator, album.artist());
        generator.writeEndObject();
    }

    private void writeArtistTasks(
            JsonGenerator generator,
            String discoveryId,
            LocalDateTime maximumCreationDate
    ) {
        generator.writeArrayPropertyStart("tasks");
        String afterTaskId = "";
        while (true) {
            List<TaskExportDto> tasks = findArtistTaskPage(discoveryId, afterTaskId, maximumCreationDate);
            if (tasks.isEmpty()) {
                break;
            }
            for (TaskExportDto task : tasks) {
                generator.writePOJO(task);
            }
            generator.flush();
            afterTaskId = tasks.getLast().id();
        }
        generator.writeEndArray();
    }

    private void writeAlbumTasks(
            JsonGenerator generator,
            String discoveryId,
            LocalDateTime maximumCreationDate
    ) {
        generator.writeArrayPropertyStart("tasks");
        String afterTaskId = "";
        while (true) {
            List<TaskExportDto> tasks = findAlbumTaskPage(discoveryId, afterTaskId, maximumCreationDate);
            if (tasks.isEmpty()) {
                break;
            }
            for (TaskExportDto task : tasks) {
                generator.writePOJO(task);
            }
            generator.flush();
            afterTaskId = tasks.getLast().id();
        }
        generator.writeEndArray();
    }

    private void writeArtistGenres(JsonGenerator generator, String artistId) {
        generator.writeArrayPropertyStart("genres");
        String afterGenreId = "";
        while (true) {
            List<GenreExportDto> genres = findGenrePage(artistId, afterGenreId);
            if (genres.isEmpty()) {
                break;
            }
            for (GenreExportDto genre : genres) {
                generator.writePOJO(genre);
            }
            generator.flush();
            afterGenreId = genres.getLast().id();
        }
        generator.writeEndArray();
    }

    private ArtistDiscoveryPageDto findArtistDiscoveryPage(String afterArtistId, LocalDateTime maximumCreationDate) {
        return inReadTransaction(() -> {
            List<Artist> artists = artistRepository.findForEvaluation(afterArtistId, ENTITY_PAGE);
            List<ArtistDiscoveryExportDto> discoveries = new ArrayList<>();
            for (Artist artist : artists) {
                artistDiscoveryRepository.findLatestForEvaluation(artist.getId(), maximumCreationDate, Limit.of(1))
                        .map(this::toArtistDiscoveryDto).ifPresent(discoveries::add);
            }
            String lastArtistId = artists.isEmpty() ? null : artists.getLast().getId();
            return new ArtistDiscoveryPageDto(discoveries, lastArtistId);
        });
    }

    private AlbumDiscoveryPageDto findAlbumDiscoveryPage(String afterAlbumId, LocalDateTime maximumCreationDate) {
        return inReadTransaction(() -> {
            List<Album> albums = albumRepository.findForEvaluation(afterAlbumId, ENTITY_PAGE);
            List<AlbumDiscoveryExportDto> discoveries = new ArrayList<>();
            for (Album album : albums) {
                albumDiscoveryRepository.findLatestForEvaluation(album.getId(), maximumCreationDate, Limit.of(1))
                        .map(this::toAlbumDiscoveryDto).ifPresent(discoveries::add);
            }
            String lastAlbumId = albums.isEmpty() ? null : albums.getLast().getId();
            return new AlbumDiscoveryPageDto(discoveries, lastAlbumId);
        });
    }

    private List<TaskExportDto> findArtistTaskPage(
            String discoveryId,
            String afterTaskId,
            LocalDateTime maximumCreationDate
    ) {
        return inReadTransaction(() -> {
            List<String> taskIds = artistDiscoveryRepository.findTaskIdsForEvaluation(
                    discoveryId, afterTaskId, maximumCreationDate, TASK_PAGE);
            return loadTasks(taskIds);
        });
    }

    private List<TaskExportDto> findAlbumTaskPage(
            String discoveryId,
            String afterTaskId,
            LocalDateTime maximumCreationDate
    ) {
        return inReadTransaction(() -> {
            List<String> taskIds = albumDiscoveryRepository.findTaskIdsForEvaluation(
                    discoveryId, afterTaskId, maximumCreationDate, TASK_PAGE);
            return loadTasks(taskIds);
        });
    }

    private List<TaskExportDto> loadTasks(List<String> taskIds) {
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return discoveryTaskRepository.findForEvaluationByIds(taskIds).stream()
                .map(this::toTaskDto)
                .toList();
    }

    private List<GenreExportDto> findGenrePage(String artistId, String afterGenreId) {
        return inReadTransaction(() -> artistGenreRepository
                .findGenresForEvaluation(artistId, afterGenreId, ENTITY_PAGE)
                .stream()
                .map(this::toGenreDto)
                .toList());
    }

    private <T> T inReadTransaction(Supplier<T> supplier) {
        return requireNonNull(transactionTemplate.execute(status -> supplier.get()));
    }

    private ArtistDiscoveryExportDto toArtistDiscoveryDto(ArtistDiscovery discovery) {
        return new ArtistDiscoveryExportDto(
                discovery.getId(),
                discovery.getCreationDate().toString(),
                discovery.getUpdateDate() != null ? discovery.getUpdateDate().toString() : null,
                discovery.getJob().getId(),
                toArtistDto(discovery.getArtist())
        );
    }

    private AlbumDiscoveryExportDto toAlbumDiscoveryDto(AlbumDiscovery discovery) {
        return new AlbumDiscoveryExportDto(
                discovery.getId(),
                discovery.getCreationDate().toString(),
                discovery.getUpdateDate() != null ? discovery.getUpdateDate().toString() : null,
                discovery.getJob().getId(),
                toAlbumDto(discovery.getAlbum())
        );
    }

    private ArtistExportDto toArtistDto(Artist artist) {
        return new ArtistExportDto(
                artist.getId(),
                artist.getCreationDate().toString(),
                artist.getUpdateDate() != null ? artist.getUpdateDate().toString() : null,
                artist.getName(),
                artist.getArtwork() != null ? artist.getArtwork().getId() : null
        );
    }

    private AlbumExportDto toAlbumDto(Album album) {
        return new AlbumExportDto(
                album.getId(),
                album.getCreationDate().toString(),
                album.getUpdateDate() != null ? album.getUpdateDate().toString() : null,
                album.getName(),
                album.getArtwork() != null ? album.getArtwork().getId() : null,
                album.getYear(),
                toArtistDto(album.getArtist())
        );
    }

    private GenreExportDto toGenreDto(Genre genre) {
        return new GenreExportDto(
                genre.getId(),
                genre.getCreationDate().toString(),
                genre.getUpdateDate() != null ? genre.getUpdateDate().toString() : null,
                genre.getName(),
                genre.getArtwork() != null ? genre.getArtwork().getId() : null
        );
    }

    private TaskExportDto toTaskDto(DiscoveryTask task) {
        return new TaskExportDto(
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
        );
    }

    private record ArtistExportDto(
            String id,
            String creationDate,
            @Nullable String updateDate,
            @Nullable String name,
            @Nullable String artworkId
    ) {}

    private record AlbumExportDto(
            String id,
            String creationDate,
            @Nullable String updateDate,
            @Nullable String name,
            @Nullable String artworkId,
            @Nullable Integer year,
            ArtistExportDto artist
    ) {}

    private record ArtistDiscoveryExportDto(
            String id,
            String creationDate,
            @Nullable String updateDate,
            String jobId,
            ArtistExportDto artist
    ) {}

    private record AlbumDiscoveryExportDto(
            String id,
            String creationDate,
            @Nullable String updateDate,
            String jobId,
            AlbumExportDto album
    ) {}

    private record ArtistDiscoveryPageDto(List<ArtistDiscoveryExportDto> discoveries, @Nullable String lastArtistId) {}

    private record AlbumDiscoveryPageDto(List<AlbumDiscoveryExportDto> discoveries, @Nullable String lastAlbumId) {}

    private record GenreExportDto(
            String id,
            String creationDate,
            @Nullable String updateDate,
            @Nullable String name,
            @Nullable String artworkId
    ) {}

    private record TaskExportDto(
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
