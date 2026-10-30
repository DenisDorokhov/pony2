package net.dorokhov.pony2.web.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.*;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class LlmEvaluationExportService {

    private static final PageRequest ENTITY_PAGE = PageRequest.of(0, 100);
    private static final List<DiscoveryTaskType> TASK_TYPES = Arrays.stream(DiscoveryTaskType.values())
            .sorted(Comparator.comparing(DiscoveryTaskType::name)).toList();

    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final DiscoveryTaskRepository discoveryTaskRepository;
    private final ArtistGenreRepository artistGenreRepository;
    private final JsonMapper jsonMapper;
    private final ObjectWriter writer;
    private final TransactionTemplate transactionTemplate;

    public LlmEvaluationExportService(
            GenreRepository genreRepository,
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            SongRepository songRepository,
            DiscoveryTaskRepository discoveryTaskRepository,
            ArtistGenreRepository artistGenreRepository,
            PlatformTransactionManager transactionManager,
            JsonMapper jsonMapper
    ) {
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
        this.discoveryTaskRepository = discoveryTaskRepository;
        this.artistGenreRepository = artistGenreRepository;
        this.jsonMapper = jsonMapper;
        // An interrupted export must remain incomplete, and the servlet owns the output stream.
        writer = jsonMapper.writerWithDefaultPrettyPrinter().withoutFeatures(
                StreamWriteFeature.AUTO_CLOSE_CONTENT, StreamWriteFeature.AUTO_CLOSE_TARGET);
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW);
        transactionDefinition.setReadOnly(true);
        transactionTemplate = new TransactionTemplate(transactionManager, transactionDefinition);
    }

    public void write(OutputStream outputStream, LocalDateTime exportedAt) {
        try (JsonGenerator generator = writer.createGenerator(outputStream)) {
            generator.writeStartObject();
            generator.writeStringProperty("exportedAt", exportedAt.toString());
            generator.flush();
            writeGenres(generator);
            writeArtists(generator);
            writeAlbums(generator);
            writeSongs(generator);
            generator.writeEndObject();
        }
    }

    private void writeGenres(JsonGenerator generator) {
        generator.writeArrayPropertyStart("genres");
        String afterId = "";
        while (true) {
            String cursor = afterId;
            List<String> ids = inReadTransaction(() -> discoveryTaskRepository.findGenreIdsForEvaluation(
                    cursor, ENTITY_PAGE));
            if (ids.isEmpty()) {
                break;
            }
            for (String id : ids) {
                GenreExportDto object = inReadTransaction(() -> toGenreDto(genreRepository.findById(id).orElseThrow()));
                writeGenre(generator, object, true);
                generator.flush();
            }
            afterId = ids.getLast();
        }
        generator.writeEndArray();
    }

    private void writeArtists(JsonGenerator generator) {
        generator.writeArrayPropertyStart("artists");
        String afterId = "";
        while (true) {
            String cursor = afterId;
            List<String> ids = inReadTransaction(() -> discoveryTaskRepository.findArtistIdsForEvaluation(
                    cursor, ENTITY_PAGE));
            if (ids.isEmpty()) {
                break;
            }
            for (String id : ids) {
                ArtistExportDto object = inReadTransaction(() -> toArtistDto(artistRepository.findById(id).orElseThrow()));
                writeArtist(generator, object, true);
                generator.flush();
            }
            afterId = ids.getLast();
        }
        generator.writeEndArray();
    }

    private void writeAlbums(JsonGenerator generator) {
        generator.writeArrayPropertyStart("albums");
        String afterId = "";
        while (true) {
            String cursor = afterId;
            List<String> ids = inReadTransaction(() -> discoveryTaskRepository.findAlbumIdsForEvaluation(
                    cursor, ENTITY_PAGE));
            if (ids.isEmpty()) {
                break;
            }
            for (String id : ids) {
                AlbumExportDto object = inReadTransaction(() -> toAlbumDto(albumRepository.findById(id).orElseThrow()));
                writeAlbum(generator, object, true);
                generator.flush();
            }
            afterId = ids.getLast();
        }
        generator.writeEndArray();
    }

    private void writeSongs(JsonGenerator generator) {
        generator.writeArrayPropertyStart("songs");
        String afterId = "";
        while (true) {
            String cursor = afterId;
            List<String> ids = inReadTransaction(() -> discoveryTaskRepository.findSongIdsForEvaluation(
                    cursor, ENTITY_PAGE));
            if (ids.isEmpty()) {
                break;
            }
            for (String id : ids) {
                SongExportDto object = inReadTransaction(() -> toSongDto(songRepository.findById(id).orElseThrow()));
                writeSong(generator, object);
                generator.flush();
            }
            afterId = ids.getLast();
        }
        generator.writeEndArray();
    }

    private void writeGenre(JsonGenerator generator, GenreExportDto genre, boolean includeTasks) {
        generator.writeStartObject();
        generator.writeStringProperty("id", genre.id());
        generator.writeStringProperty("creationDate", genre.creationDate());
        generator.writeStringProperty("updateDate", genre.updateDate());
        generator.writeStringProperty("name", genre.name());
        generator.writeStringProperty("artworkId", genre.artworkId());
        if (includeTasks) {
            writeTasks(generator, type -> discoveryTaskRepository.findLatestCompletedByGenreIdAndType(
                    genre.id(), type, Limit.of(1)));
        }
        generator.writeEndObject();
    }

    private void writeArtist(JsonGenerator generator, ArtistExportDto artist, boolean includeTasks) {
        generator.writeStartObject();
        generator.writeStringProperty("id", artist.id());
        generator.writeStringProperty("creationDate", artist.creationDate());
        generator.writeStringProperty("updateDate", artist.updateDate());
        generator.writeStringProperty("name", artist.name());
        generator.writeStringProperty("artworkId", artist.artworkId());
        writeArtistGenres(generator, artist.id());
        if (includeTasks) {
            writeTasks(generator, type -> discoveryTaskRepository.findLatestCompletedByArtistIdAndType(
                    artist.id(), type, Limit.of(1)));
        }
        generator.writeEndObject();
    }

    private void writeAlbum(JsonGenerator generator, AlbumExportDto album, boolean includeTasks) {
        generator.writeStartObject();
        generator.writeStringProperty("id", album.id());
        generator.writeStringProperty("creationDate", album.creationDate());
        generator.writeStringProperty("updateDate", album.updateDate());
        generator.writeStringProperty("name", album.name());
        generator.writeStringProperty("artworkId", album.artworkId());
        generator.writeName("year");
        generator.writePOJO(album.year());
        generator.writeName("artist");
        writeArtist(generator, album.artist(), false);
        if (includeTasks) {
            writeTasks(generator, type -> discoveryTaskRepository.findLatestCompletedByAlbumIdAndType(
                    album.id(), type, Limit.of(1)));
        }
        generator.writeEndObject();
    }

    private void writeSong(JsonGenerator generator, SongExportDto song) {
        generator.writeStartObject();
        generator.writeStringProperty("id", song.id());
        generator.writeStringProperty("creationDate", song.creationDate());
        generator.writeStringProperty("updateDate", song.updateDate());
        generator.writeStringProperty("name", song.name());
        generator.writeStringProperty("artworkId", song.artworkId());
        generator.writeName("year");
        generator.writePOJO(song.year());
        generator.writeName("artist");
        writeArtist(generator, song.artist(), false);
        generator.writeName("discNumber");
        generator.writePOJO(song.discNumber());
        generator.writeName("trackNumber");
        generator.writePOJO(song.trackNumber());
        generator.writeName("album");
        writeAlbum(generator, song.album(), false);
        generator.writeName("genre");
        writeGenre(generator, song.genre(), false);
        writeTasks(generator, type -> discoveryTaskRepository.findLatestCompletedBySongIdAndType(
                song.id(), type, Limit.of(1)));
        generator.writeEndObject();
    }

    private void writeTasks(JsonGenerator generator, Function<DiscoveryTaskType, Optional<DiscoveryTask>> taskFinder) {
        generator.writeArrayPropertyStart("tasks");
        for (DiscoveryTaskType type : TASK_TYPES) {
            Optional<TaskExportDto> task = inReadTransaction(() -> taskFinder.apply(type).map(this::toTaskDto));
            if (task.isPresent()) {
                generator.writePOJO(task.get());
                generator.flush();
            }
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

    private SongExportDto toSongDto(Song song) {
        return new SongExportDto(song.getId(), song.getCreationDate().toString(),
                song.getUpdateDate() != null ? song.getUpdateDate().toString() : null,
                song.getName(), song.getArtwork() != null ? song.getArtwork().getId() : null,
                song.getYear(), song.getDiscNumber(), song.getTrackNumber(),
                toArtistDto(song.getArtist()), toAlbumDto(song.getAlbum()), toGenreDto(song.getGenre()));
    }

    private TaskExportDto toTaskDto(DiscoveryTask task) {
        return new TaskExportDto(task.getId(), task.getCreationDate().toString(),
                task.getUpdateDate() != null ? task.getUpdateDate().toString() : null,
                task.getJob().getId(), task.getType().name(), task.getStatus().name(),
                task.getResult() != null ? jsonMapper.readTree(task.getResult()) : null,
                task.getRawRequests() != null ? JsonConverter.toJson(task.getRawRequests()) : null,
                task.getRawResults() != null ? JsonConverter.toJson(task.getRawResults()) : null);
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

    private record GenreExportDto(
            String id,
            String creationDate,
            @Nullable String updateDate,
            @Nullable String name,
            @Nullable String artworkId
    ) {}

    private record SongExportDto(
            String id,
            String creationDate,
            @Nullable String updateDate,
            @Nullable String name,
            @Nullable String artworkId,
            @Nullable Integer year,
            @Nullable Integer discNumber,
            @Nullable Integer trackNumber,
            ArtistExportDto artist,
            AlbumExportDto album,
            GenreExportDto genre
    ) {}

    private record TaskExportDto(
            String id,
            String creationDate,
            @Nullable String updateDate,
            String jobId,
            String type,
            String status,
            @Nullable JsonNode result,
            @Nullable String rawRequests,
            @Nullable String rawResults
    ) {}
}
