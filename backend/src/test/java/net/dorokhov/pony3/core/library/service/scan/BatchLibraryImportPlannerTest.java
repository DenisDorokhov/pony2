package net.dorokhov.pony3.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony3.core.library.repository.SongRepository;
import net.dorokhov.pony3.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony3.core.library.service.scan.BatchLibraryImportPlanner.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static net.dorokhov.pony3.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BatchLibraryImportPlannerTest {

    @InjectMocks
    private BatchLibraryImportPlanner batchLibraryImportPlanner;
    
    @Mock
    private SongRepository songRepository;
    
    @Mock
    private AudioNode audioNode;

    @TempDir
    public Path tempFolder;

    @BeforeEach
    public void setUp() throws IOException {
        when(audioNode.getFile()).thenAnswer(invocationOnMock -> Files.createFile(tempFolder.resolve(UUID.randomUUID().toString())).toFile());
    }

    @Test
    public void shouldPlanImportOfNonExistentSongs() {

        when(songRepository.findByPath(any())).thenReturn(null);

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).containsExactly(audioNode);
        assertThat(plan.getAudioNodesToSkip()).isEmpty();
    }

    @Test
    public void shouldPlanImportOfSongsOutdatedByCreationDate() {

        String songPath = audioNode.getFile().getAbsolutePath();
        when(songRepository.findByPath(any())).thenReturn(song()
                .setCreationDate(LocalDateTime.now().minusDays(1))
                .setUpdateDate(null)
                .setPath(songPath));

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).containsExactly(audioNode);
        assertThat(plan.getAudioNodesToSkip()).isEmpty();
    }

    @Test
    public void shouldPlanImportOfSongsOutdatedByUpdateDate() {

        String songPath = audioNode.getFile().getAbsolutePath();
        when(songRepository.findByPath(any())).thenReturn(song()
                .setCreationDate(LocalDateTime.now().minusDays(2))
                .setUpdateDate(LocalDateTime.now().minusDays(1))
                .setPath(songPath));

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).containsExactly(audioNode);
        assertThat(plan.getAudioNodesToSkip()).isEmpty();
    }

    @Test
    public void shouldSkipUpToDateSongs() {

        String songPath = audioNode.getFile().getAbsolutePath();
        when(songRepository.findByPath(any())).thenReturn(song()
                .setCreationDate(LocalDateTime.now().plusDays(1))
                .setUpdateDate(null)
                .setPath(songPath));

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).isEmpty();
        assertThat(plan.getAudioNodesToSkip()).containsExactly(audioNode);
    }
}