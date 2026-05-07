package net.dorokhov.pony2.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony2.core.library.service.scan.BatchLibraryImportPlanner.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        File file = Files.createFile(tempFolder.resolve(UUID.randomUUID().toString())).toFile();
        when(audioNode.getFile()).thenAnswer(invocationOnMock -> file);
    }

    @Test
    public void shouldPlanImportOfNonExistentSongs() {

        when(songRepository.findByPathIn(any())).thenReturn(List.of());

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).containsExactly(audioNode);
        assertThat(plan.getAudioNodesToSkip()).isEmpty();
    }

    @Test
    public void shouldPlanImportOfSongsOutdatedByCreationDate() {

        String songPath = audioNode.getFile().getAbsolutePath();
        when(songRepository.findByPathIn(any())).thenReturn(List.of(new SongRepository.SongFile(
                songPath,
                LocalDateTime.now().minusDays(1),
                null
        )));

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).containsExactly(audioNode);
        assertThat(plan.getAudioNodesToSkip()).isEmpty();
    }

    @Test
    public void shouldPlanImportOfSongsOutdatedByUpdateDate() {

        String songPath = audioNode.getFile().getAbsolutePath();
        when(songRepository.findByPathIn(any())).thenReturn(List.of(new SongRepository.SongFile(
                songPath,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1)
        )));

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).containsExactly(audioNode);
        assertThat(plan.getAudioNodesToSkip()).isEmpty();
    }

    @Test
    public void shouldSkipUpToDateSongs() {

        String songPath = audioNode.getFile().getAbsolutePath();
        when(songRepository.findByPathIn(any())).thenReturn(List.of(new SongRepository.SongFile(
                songPath,
                LocalDateTime.now().plusDays(1),
                null
        )));

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).isEmpty();
        assertThat(plan.getAudioNodesToSkip()).containsExactly(audioNode);
    }
}