package net.dorokhov.pony3.core.library.service;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import net.dorokhov.pony3.api.library.domain.*;
import net.dorokhov.pony3.api.library.service.ExportService;
import net.dorokhov.pony3.core.library.repository.SongRepository;
import org.apache.tika.io.FilenameUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import static java.util.Collections.unmodifiableList;

@Service
public class ExportServiceImpl implements ExportService {

    static final String UNKNOWN_ARTIST = "Unknown Artist";
    static final String UNKNOWN_ALBUM = "Unknown Album";

    private static final FileType ARCHIVE_FILE_TYPE = FileType.of("application/zip", "zip");

    private final SongRepository songRepository;

    public ExportServiceImpl(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public ExportBundle exportSong(String id) {

        Song song = songRepository.findById(id).orElse(null);
        if (song == null) {
            return null;
        }
        File file = song.getFile();
        if (!file.exists()) {
            return null;
        }

        String baseName;
        if (song.getName() != null) {
            if (song.getAlbum().getArtist().getName() != null) {
                baseName = String.format("%s - %s",
                        song.getAlbum().getArtist().getName(),
                        song.getName());
            } else {
                baseName = song.getName();
            }
        } else {
            baseName = Files.getNameWithoutExtension(song.getPath());
        }
        return buildExportBundle(FilenameUtils.normalize(baseName), song.getFileType(), new Mp3Content(file));
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public ExportBundle exportAlbum(String id) {

        List<Song> songList = songRepository.findByAlbumId(id,
                Sort.by("discNumber", "trackNumber", "name"));
        if (songList.isEmpty()) {
            return null;
        }

        Song firstSong = songList.getFirst();
        String baseName = FilenameUtils.normalize(String.format("%s - %s", 
                buildArtistFileName(firstSong.getAlbum().getArtist()), 
                buildAlbumFileName(firstSong.getAlbum())));
        return buildExportBundle(baseName, ARCHIVE_FILE_TYPE, new ZipContent(songsToZipEntries(songList)));
    }

    @Override
    @Transactional(readOnly = true)
    @Nullable
    public ExportBundle exportArtist(String id) {

        List<Song> songList = songRepository.findByAlbumArtistId(id,
                Sort.by("album.year", "album.name", "discNumber", "trackNumber", "name"));
        if (songList.isEmpty()) {
            return null;
        }

        Song firstSong = songList.getFirst();
        String baseName = FilenameUtils.normalize(buildArtistFileName(firstSong.getAlbum().getArtist()));
        return buildExportBundle(baseName, ARCHIVE_FILE_TYPE, new ZipContent(songsToZipEntries(songList)));
    }

    private String buildArtistFileName(Artist artist) {
        return artist.getName() != null ? artist.getName() : UNKNOWN_ARTIST;
    }

    private String buildAlbumFileName(Album album) {
        String albumName = album.getName() != null ? album.getName() : UNKNOWN_ALBUM;
        if (album.getYear() != null) {
            return String.format("%d - %s", album.getYear(), albumName);
        } else {
            return albumName;
        }
    }

    private String buildSongFileName(Song song) {
        if (song.getName() != null) {
            if (song.getTrackNumber() != null) {
                return String.format("%s - %s", buildTrackNumber(song.getTrackNumber()), song.getName());
            } else {
                return song.getName();
            }
        } else {
            return Files.getNameWithoutExtension(song.getFile().getName());
        }
    }

    private String buildTrackNumber(Integer trackNumber) {
        return trackNumber <= 9 ? "0" + trackNumber : String.valueOf(trackNumber);
    }

    private List<ZipEntry> songsToZipEntries(List<Song> songList) {

        Map<String, Integer> albumToDiscCount = new HashMap<>();
        for (Song song : songList) {
            Integer discCount = albumToDiscCount.get(song.getAlbum().getId());
            if (song.getDiscNumber() != null && song.getDiscNumber() > 1) {
                discCount = song.getDiscNumber();
            }
            if (discCount == null) {
                discCount = 1;
            }
            albumToDiscCount.put(song.getAlbum().getId(), discCount);
        }

        ImmutableList.Builder<ZipEntry> builder = ImmutableList.builder();
        for (Song song : songList) {
            File file = song.getFile();
            if (!file.exists()) {
                continue;
            }
            String currentArtistFileName = buildArtistFileName(song.getAlbum().getArtist());
            String currentAlbumFileName = buildAlbumFileName(song.getAlbum());
            Path path = Paths.get(currentArtistFileName, currentAlbumFileName);
            int discCount = albumToDiscCount.get(song.getAlbum().getId());
            if (discCount > 1) {
                int discNumber = song.getDiscNumber() != null ? song.getDiscNumber() : 1;
                path = path.resolve("CD" + discNumber);
            }
            path = path.resolve(buildSongFileName(song));
            builder.add(buildUniqueZipEntry(file, path, builder.build()));
        }
        return builder.build();
    }

    @SuppressWarnings("IdempotentLoopBody")
    private ZipEntry buildUniqueZipEntry(File file, Path path, List<ZipEntry> items) {

        Set<String> existingPaths = items.stream()
                .map(ZipEntry::getPath)
                .map(Path::toString)
                .collect(Collectors.toSet());
        String extension = Files.getFileExtension(file.getName()).toLowerCase();

        int attempt = 1;
        String currentExportPath = path.toString() + "." + extension;
        while (existingPaths.contains(currentExportPath)) {
            currentExportPath = String.format("%s (%s).%s", path, attempt, extension);
        }
        return new ZipEntry(file, Paths.get(currentExportPath));
    }
    
    private ExportBundle buildExportBundle(String baseName, FileType fileType, ExportBundle.Content content) {
        return new ExportBundle(baseName + "." + fileType.getFileExtension(), fileType.getMimeType(), content);
    }

    static final class Mp3Content implements ExportBundle.Content {

        private final File file;

        public Mp3Content(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                fileInputStream.transferTo(outputStream);
            }
        }
    }

    static final class ZipContent implements ExportBundle.Content {

        private final List<ZipEntry> entries;

        public ZipContent(List<ZipEntry> entries) {
            this.entries = unmodifiableList(entries);
        }

        public List<ZipEntry> getEntries() {
            return entries;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (ZipEntry entry : entries) {
                    compressFile(entry.getFile(), entry.getPath(), zipOutputStream);
                }
            }
        }

        private void compressFile(File file, Path path, ZipOutputStream zipOutputStream) throws IOException {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                zipOutputStream.putNextEntry(new java.util.zip.ZipEntry(path.toString()));
                int len;
                byte[] buf = new byte[1024];
                while ((len = fileInputStream.read(buf)) > 0) {
                    zipOutputStream.write(buf, 0, len);
                }
                zipOutputStream.closeEntry();
            }
        }
    }

    static final class ZipEntry {

        private final File file;
        private final Path path;

        public ZipEntry(File file, Path path) {
            this.file = file;
            this.path = path;
        }

        public File getFile() {
            return file;
        }

        public Path getPath() {
            return path;
        }
    }
}
