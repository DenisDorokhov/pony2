package net.dorokhov.pony2.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.ExportServiceImpl.Mp3Content;
import net.dorokhov.pony2.core.library.service.ExportServiceImpl.ZipContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony2.core.library.service.ExportServiceImpl.UNKNOWN_ALBUM;
import static net.dorokhov.pony2.core.library.service.ExportServiceImpl.UNKNOWN_ARTIST;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExportServiceImplTest {

    private static final Resource SONG_RESOURCE = new ClassPathResource("audio/empty.mp3");

    @TempDir
    private Path tempDir;

    @InjectMocks
    private ExportServiceImpl exportService;

    @Mock
    private SongRepository songRepository;

    @Test
    public void shouldExportSong() throws IOException {

        File songFile = SONG_RESOURCE.getFile();
        Song song = song()
                .setAlbum(new Album()
                        .setArtist(new Artist()
                                .setName("someArtist"))
                        .setName("someAlbum"))
                .setName("someSong")
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setPath(songFile.getPath());
        when(songRepository.findById(any())).thenReturn(Optional.of(song));

        ExportBundle exportBundle = exportService.exportSong("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo("someArtist - someSong.mp3");
        checkSongExportBundle(exportBundle, mp3Content ->
                assertThat(mp3Content.getFile()).isEqualTo(songFile));
    }

    @Test
    public void shouldExportSongWithoutName() throws IOException {
        
        File songFile = SONG_RESOURCE.getFile();

        Song song = song()
                .setAlbum(new Album()
                        .setArtist(new Artist()))
                .setName(null)
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setPath(songFile.getPath());
        when(songRepository.findById(any())).thenReturn(Optional.of(song));

        ExportBundle exportBundle = exportService.exportSong("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo(songFile.getName());
        checkSongExportBundle(exportBundle, mp3Content ->
                assertThat(mp3Content.getFile()).isEqualTo(songFile));
    }

    @Test
    public void shouldExportSongWithUnknownArtist() throws IOException {

        File songFile = SONG_RESOURCE.getFile();

        Song song = song()
                .setAlbum(new Album()
                        .setArtist(new Artist()))
                .setName("someSong")
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setPath(songFile.getPath());
        when(songRepository.findById(any())).thenReturn(Optional.of(song));

        ExportBundle exportBundle = exportService.exportSong("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo("someSong.mp3");
        checkSongExportBundle(exportBundle, mp3Content ->
                assertThat(mp3Content.getFile()).isEqualTo(songFile));
    }

    @Test
    public void shouldExportAlbum() throws IOException {
        
        File songFile1 = Files.createFile(tempDir.resolve("song1.mp3")).toFile();
        File songFile2 = Files.createFile(tempDir.resolve("song2.mp3")).toFile();
        File songFile3 = Files.createFile(tempDir.resolve("song3.mp3")).toFile();
        File songFile4 = new File("notExistingFile.mp3");

        Album album = new Album()
                .setArtist(new Artist()
                        .setName("someArtist"))
                .setName("someAlbum")
                .setYear(1986);

        Song song1 = song()
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setAlbum(album)
                .setName("song1")
                .setDiscNumber(1)
                .setTrackNumber(1)
                .setPath(songFile1.getPath());
        Song song2 = song()
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setAlbum(album)
                .setName("song2")
                .setDiscNumber(null)
                .setTrackNumber(12)
                .setPath(songFile2.getPath());
        Song song3 = song()
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setAlbum(album)
                .setName("song3")
                .setDiscNumber(2)
                .setTrackNumber(null)
                .setPath(songFile3.getPath());
        Song song4 = song()
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setAlbum(album)
                .setName("song4")
                .setDiscNumber(null)
                .setTrackNumber(null)
                .setPath(songFile4.getPath());
        when(songRepository.findByAlbumId(any(), (Sort) any())).thenReturn(ImmutableList.of(song1, song2, song3, song4));

        ExportBundle exportBundle = exportService.exportAlbum("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo("someArtist - 1986 - someAlbum.zip");
        checkZipExportBundle(exportBundle, zipContent -> {
            assertThat(zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getFile)
                    .toList())
                    .containsExactly(
                            songFile1,
                            songFile2,
                            songFile3
                    );
            assertThat(zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getPath)
                    .toList())
                    .containsExactly(
                            Paths.get("someArtist/1986 - someAlbum/CD1/01 - song1.mp3"),
                            Paths.get("someArtist/1986 - someAlbum/CD1/12 - song2.mp3"),
                            Paths.get("someArtist/1986 - someAlbum/CD2/song3.mp3")
                    );
        });
    }

    @Test
    public void shouldExportAlbumWithoutName() throws IOException {
        
        File songFile1 = Files.createFile(tempDir.resolve("song1.mp3")).toFile();

        Song song1 = song()
                .setAlbum(new Album()
                        .setArtist(new Artist()))
                .setDiscNumber(null)
                .setTrackNumber(null)
                .setName(null)
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setPath(songFile1.getPath());
        Song song2 = song()
                .setAlbum(new Album()
                        .setArtist(new Artist()))
                .setDiscNumber(null)
                .setTrackNumber(null)
                .setName(null)
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setPath(songFile1.getPath());
        when(songRepository.findByAlbumId(any(), (Sort) any())).thenReturn(ImmutableList.of(song1, song2));

        ExportBundle exportBundle = exportService.exportAlbum("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getContent()).isInstanceOfSatisfying(ZipContent.class, zipContent -> {
            List<String> paths = zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getPath)
                    .map(Path::toString)
                    .toList();
            assertThat(paths).containsExactly(
                    UNKNOWN_ARTIST + File.separator + UNKNOWN_ALBUM + File.separator + "song1.mp3",
                    UNKNOWN_ARTIST + File.separator + UNKNOWN_ALBUM + File.separator + "song1 (1).mp3"
            );
        });
    }

    @Test
    public void shouldExportArtist() throws IOException {
        
        File songFile1 = Files.createFile(tempDir.resolve("song1.mp3")).toFile();
        File songFile2 = Files.createFile(tempDir.resolve("song2.mp3")).toFile();
        File songFile3 = Files.createFile(tempDir.resolve("song3.mp3")).toFile();
        File songFile4 = new File("notExistingSong.mp3");

        Artist artist = new Artist()
                .setName("someArtist");

        Album album1 = new Album()
                .setArtist(artist)
                .setName("someAlbum1")
                .setYear(1986);
        Album album2 = new Album()
                .setArtist(artist)
                .setName("someAlbum2")
                .setYear(1987);

        Song song1 = song()
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setAlbum(album1)
                .setDiscNumber(1)
                .setName("song1")
                .setTrackNumber(1)
                .setPath(songFile1.getPath());
        Song song2 = song()
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setAlbum(album1)
                .setDiscNumber(1)
                .setName("song2")
                .setTrackNumber(12)
                .setPath(songFile2.getPath());
        Song song3 = song()
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setAlbum(album2)
                .setDiscNumber(1)
                .setName("song3")
                .setTrackNumber(1)
                .setPath(songFile3.getPath());
        Song song4 = song()
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setAlbum(album1)
                .setDiscNumber(1)
                .setName("song4")
                .setTrackNumber(1)
                .setPath(songFile4.getPath());
        when(songRepository.findByAlbumArtistId(any(), (Sort) any())).thenReturn(ImmutableList.of(song1, song2, song3, song4));

        ExportBundle exportBundle = exportService.exportArtist("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo("someArtist.zip");
        checkZipExportBundle(exportBundle, zipContent -> {
            assertThat(zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getFile)
                    .toList())
                    .containsExactly(songFile1, songFile2, songFile3);
            assertThat(zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getPath)
                    .toList())
                    .containsExactly(
                            Paths.get("someArtist/1986 - someAlbum1/01 - song1.mp3"),
                            Paths.get("someArtist/1986 - someAlbum1/12 - song2.mp3"),
                            Paths.get("someArtist/1987 - someAlbum2/01 - song3.mp3")
                    );
        });
    }

    @Test
    public void shouldReturnNullIfSongNotFound() {

        when(songRepository.findById(any())).thenReturn(Optional.empty());
        
        assertThat(exportService.exportSong("1")).isNull();
    }

    @Test
    public void shouldReturnNullIfSongFileNotFound() {

        Song song = song()
                .setAlbum(new Album()
                        .setArtist(new Artist()
                                .setName("someArtist"))
                        .setName("someAlbum"))
                .setName("someSong")
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setPath("foo/bar.mp3");
        when(songRepository.findById(any())).thenReturn(Optional.ofNullable(song));
        
        assertThat(exportService.exportSong("1")).isNull();
    }

    @Test
    public void shouldReturnNullIfAlbumNotFound() {

        when(songRepository.findByAlbumId(any(), (Sort) any())).thenReturn(emptyList());

        assertThat(exportService.exportAlbum("1")).isNull();
    }

    @Test
    public void shouldFailIfArtistNotFound() {

        when(songRepository.findByAlbumArtistId(any(), (Sort) any())).thenReturn(emptyList());

        assertThat(exportService.exportArtist("1")).isNull();
    }

    @Test
    public void shouldWriteMp3Content() throws IOException {

        File sourceFile = SONG_RESOURCE.getFile();
        File targetFile = Files.createFile(tempDir.resolve("target.mp3")).toFile();

        try (OutputStream outputStream = new FileOutputStream(targetFile)) {
            new Mp3Content(sourceFile).write(outputStream);
        }

        try (
                FileInputStream sourceStream = new FileInputStream(sourceFile);
                FileInputStream targetStream = new FileInputStream(targetFile)
        ) {
            assertThat(DigestUtils.md5Digest(sourceStream))
                    .isEqualTo(DigestUtils.md5Digest(targetStream));
        }
    }

    @Test
    public void shouldWriteZipContent() throws IOException {

        File sourceFile = SONG_RESOURCE.getFile();
        ZipContent zipContent = new ZipContent(ImmutableList.of(
                new ExportServiceImpl.ZipEntry(sourceFile, Paths.get("song1.mp3")),
                new ExportServiceImpl.ZipEntry(sourceFile, Paths.get("foo/song2.mp3")),
                new ExportServiceImpl.ZipEntry(sourceFile, Paths.get("foo/bar/song3.mp3")),
                new ExportServiceImpl.ZipEntry(sourceFile, Paths.get("foo/bar/song4.mp3"))
        ));
        File targetFile = Files.createFile(tempDir.resolve("target.zip")).toFile();

        try (OutputStream outputStream = new FileOutputStream(targetFile)) {
            zipContent.write(outputStream);
        }

        assertThat(getFileListFromZip(targetFile))
                .contains(
                        "song1.mp3",
                        "foo" + File.separator + "song2.mp3",
                        "foo" + File.separator + "bar" + File.separator + "song3.mp3",
                        "foo" + File.separator + "bar" + File.separator + "song4.mp3"
                );
    }

    private void checkSongExportBundle(ExportBundle exportBundle, Consumer<Mp3Content> contentChecker) {
        assertThat(exportBundle.getMimeType()).isEqualTo("audio/mpeg");
        assertThat(exportBundle.getContent()).isInstanceOfSatisfying(Mp3Content.class, contentChecker);
    }

    private void checkZipExportBundle(ExportBundle exportBundle, Consumer<ZipContent> contentChecker) {
        assertThat(exportBundle.getMimeType()).isEqualTo("application/zip");
        assertThat(exportBundle.getContent()).isInstanceOf(ZipContent.class);
        assertThat(exportBundle.getContent()).isInstanceOfSatisfying(ZipContent.class, contentChecker);
    }

    private List<String> getFileListFromZip(File file) throws IOException {
        List<String> zippedFiles = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                zippedFiles.add(entry.getName());
                entry = zipInputStream.getNextEntry();
            }
        }
        return zippedFiles;
    }
}