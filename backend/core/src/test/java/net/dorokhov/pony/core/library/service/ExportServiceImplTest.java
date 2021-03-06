package net.dorokhov.pony.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.library.domain.*;
import net.dorokhov.pony.core.library.repository.SongRepository;
import net.dorokhov.pony.core.library.service.ExportServiceImpl.Mp3Content;
import net.dorokhov.pony.core.library.service.ExportServiceImpl.ZipContent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.core.library.service.ExportServiceImpl.UNKNOWN_ALBUM;
import static net.dorokhov.pony.core.library.service.ExportServiceImpl.UNKNOWN_ARTIST;
import static net.dorokhov.pony.test.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExportServiceImplTest {

    private static final Resource SONG_RESOURCE = new ClassPathResource("audio/empty.mp3");

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @InjectMocks
    private ExportServiceImpl exportService;

    @Mock
    private SongRepository songRepository;

    @Test
    public void shouldExportSong() throws IOException {

        File songFile = SONG_RESOURCE.getFile();
        Song song = songBuilder()
                .album(Album.builder()
                        .artist(Artist.builder()
                                .name("someArtist")
                                .build())
                        .name("someAlbum")
                        .build())
                .name("someSong")
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .path(songFile.getPath())
                .build();
        when(songRepository.findOne((String) any())).thenReturn(song);

        ExportBundle exportBundle = exportService.exportSong("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo("someArtist - someSong.mp3");
        checkSongExportBundle(exportBundle, mp3Content ->
                assertThat(mp3Content.getFile()).isEqualTo(songFile));
    }

    @Test
    public void shouldExportSongWithoutName() throws IOException {
        
        File songFile = SONG_RESOURCE.getFile();

        Song song = songBuilder()
                .album(Album.builder()
                        .artist(Artist.builder()
                                .build())
                        .build())
                .name(null)
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .path(songFile.getPath())
                .build();
        when(songRepository.findOne((String) any())).thenReturn(song);

        ExportBundle exportBundle = exportService.exportSong("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo(songFile.getName());
        checkSongExportBundle(exportBundle, mp3Content ->
                assertThat(mp3Content.getFile()).isEqualTo(songFile));
    }

    @Test
    public void shouldExportSongWithUnknownArtist() throws IOException {

        File songFile = SONG_RESOURCE.getFile();

        Song song = songBuilder()
                .album(Album.builder()
                        .artist(Artist.builder()
                                .build())
                        .build())
                .name("someSong")
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .path(songFile.getPath())
                .build();
        when(songRepository.findOne((String) any())).thenReturn(song);

        ExportBundle exportBundle = exportService.exportSong("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo("someSong.mp3");
        checkSongExportBundle(exportBundle, mp3Content ->
                assertThat(mp3Content.getFile()).isEqualTo(songFile));
    }

    @Test
    public void shouldExportAlbum() throws IOException {
        
        File songFile1 = tempFolder.newFile("song1.mp3");
        File songFile2 = tempFolder.newFile("song2.mp3");
        File songFile3 = tempFolder.newFile("song3.mp3");
        File songFile4 = new File("notExistingFile.mp3");

        Song song1 = songBuilder()
                .album(Album.builder()
                        .artist(Artist.builder()
                                .name("someArtist")
                                .build())
                        .name("someAlbum")
                        .year(1986)
                        .build())
                .discNumber(1)
                .trackNumber(1)
                .name("song1")
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .path(songFile1.getPath())
                .build();
        Song song2 = Song.builder(song1)
                .name("song2")
                .discNumber(null)
                .trackNumber(12)
                .path(songFile2.getPath())
                .build();
        Song song3 = Song.builder(song1)
                .name("song3")
                .discNumber(2)
                .trackNumber(null)
                .path(songFile3.getPath())
                .build();
        Song song4 = Song.builder(song1)
                .name("song4")
                .discNumber(null)
                .trackNumber(null)
                .path(songFile4.getPath())
                .build();
        when(songRepository.findByAlbumId(any(), (Sort) any())).thenReturn(ImmutableList.of(song1, song2, song3, song4));

        ExportBundle exportBundle = exportService.exportAlbum("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo("someArtist - 1986 - someAlbum.zip");
        checkZipExportBundle(exportBundle, zipContent -> {
            assertThat(zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getFile)
                    .collect(Collectors.toList()))
                    .containsExactly(
                            songFile1,
                            songFile2,
                            songFile3
                    );
            assertThat(zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getPath)
                    .collect(Collectors.toList()))
                    .containsExactly(
                            Paths.get("someArtist/1986 - someAlbum/CD1/01 - song1.mp3"),
                            Paths.get("someArtist/1986 - someAlbum/CD1/12 - song2.mp3"),
                            Paths.get("someArtist/1986 - someAlbum/CD2/song3.mp3")
                    );
        });
    }

    @Test
    public void shouldExportAlbumWithoutName() throws IOException {
        
        File songFile1 = tempFolder.newFile("song1.mp3");

        Song song1 = songBuilder()
                .album(Album.builder()
                        .artist(Artist.builder()
                                .build())
                        .build())
                .discNumber(null)
                .trackNumber(null)
                .name(null)
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .path(songFile1.getPath())
                .build();
        Song song2 = Song.builder(song1).build();
        when(songRepository.findByAlbumId(any(), (Sort) any())).thenReturn(ImmutableList.of(song1, song2));

        ExportBundle exportBundle = exportService.exportAlbum("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getContent()).isInstanceOfSatisfying(ZipContent.class, zipContent -> {
            List<String> paths = zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getPath)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            assertThat(paths).containsExactly(
                    UNKNOWN_ARTIST + File.separator + UNKNOWN_ALBUM + File.separator + "song1.mp3",
                    UNKNOWN_ARTIST + File.separator + UNKNOWN_ALBUM + File.separator + "song1 (1).mp3"
            );
        });
    }

    @Test
    public void shouldExportArtist() throws IOException {
        
        File songFile1 = tempFolder.newFile("song1.mp3");
        File songFile2 = tempFolder.newFile("song2.mp3");
        File songFile3 = tempFolder.newFile("song3.mp3");
        File songFile4 = new File("notExistingSong.mp3");

        Artist artist = Artist.builder()
                .name("someArtist")
                .build();

        Song song1 = songBuilder()
                .album(Album.builder()
                        .artist(artist)
                        .name("someAlbum1")
                        .year(1986)
                        .build())
                .discNumber(1)
                .trackNumber(1)
                .name("song1")
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .path(songFile1.getPath())
                .build();
        Song song2 = Song.builder(song1)
                .name("song2")
                .trackNumber(12)
                .path(songFile2.getPath())
                .build();
        Song song3 = Song.builder(song1)
                .album(Album.builder()
                        .artist(artist)
                        .name("someAlbum2")
                        .year(1987)
                        .build())
                .name("song3")
                .trackNumber(1)
                .path(songFile3.getPath())
                .build();
        Song song4 = Song.builder(song1)
                .name("song4")
                .path(songFile4.getPath())
                .build();
        when(songRepository.findByAlbumArtistId(any(), (Sort) any())).thenReturn(ImmutableList.of(song1, song2, song3, song4));

        ExportBundle exportBundle = exportService.exportArtist("1");

        assertThat(exportBundle).isNotNull();
        assertThat(exportBundle.getFileName()).isEqualTo("someArtist.zip");
        checkZipExportBundle(exportBundle, zipContent -> {
            assertThat(zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getFile)
                    .collect(Collectors.toList()))
                    .containsExactly(songFile1, songFile2, songFile3);
            assertThat(zipContent.getEntries().stream()
                    .map(ExportServiceImpl.ZipEntry::getPath)
                    .collect(Collectors.toList()))
                    .containsExactly(
                            Paths.get("someArtist/1986 - someAlbum1/01 - song1.mp3"),
                            Paths.get("someArtist/1986 - someAlbum1/12 - song2.mp3"),
                            Paths.get("someArtist/1987 - someAlbum2/01 - song3.mp3")
                    );
        });
    }

    @Test
    public void shouldReturnNullIfSongNotFound() {

        when(songRepository.findOne((String) any())).thenReturn(null);
        
        assertThat(exportService.exportSong("1")).isNull();
    }

    @Test
    public void shouldReturnNullIfSongFileNotFound() {

        Song song = songBuilder()
                .album(Album.builder()
                        .artist(Artist.builder()
                                .name("someArtist")
                                .build())
                        .name("someAlbum")
                        .build())
                .name("someSong")
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .path("foo/bar.mp3")
                .build();
        when(songRepository.findOne((String) any())).thenReturn(song);
        
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
        File targetFile = tempFolder.newFile("target.mp3");

        try (OutputStream outputStream = new FileOutputStream(targetFile)) {
            new Mp3Content(sourceFile).write(outputStream);
        }

        assertThat(DigestUtils.md5Digest(new FileInputStream(sourceFile)))
                .isEqualTo(DigestUtils.md5Digest(new FileInputStream(targetFile)));
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
        File targetFile = tempFolder.newFile("target.zip");

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