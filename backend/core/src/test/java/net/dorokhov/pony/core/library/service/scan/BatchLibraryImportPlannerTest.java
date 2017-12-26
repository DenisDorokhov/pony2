package net.dorokhov.pony.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.core.library.repository.SongRepository;
import net.dorokhov.pony.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony.core.library.service.scan.BatchLibraryImportPlanner.Plan;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.time.LocalDateTime;

import static net.dorokhov.pony.test.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BatchLibraryImportPlannerTest {

    @InjectMocks
    private BatchLibraryImportPlanner batchLibraryImportPlanner;
    
    @Mock
    private SongRepository songRepository;
    
    @Mock
    private AudioNode audioNode;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        when(audioNode.getFile()).thenReturn(tempFolder.newFile());
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
        when(songRepository.findByPath(any())).thenReturn(songBuilder()
                .creationDate(LocalDateTime.now().minusDays(1))
                .updateDate(null)
                .path(songPath)
                .build());

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).containsExactly(audioNode);
        assertThat(plan.getAudioNodesToSkip()).isEmpty();
    }

    @Test
    public void shouldPlanImportOfSongsOutdatedByUpdateDate() {

        String songPath = audioNode.getFile().getAbsolutePath();
        when(songRepository.findByPath(any())).thenReturn(songBuilder()
                .creationDate(LocalDateTime.now().minusDays(2))
                .updateDate(LocalDateTime.now().minusDays(1))
                .path(songPath)
                .build());

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).containsExactly(audioNode);
        assertThat(plan.getAudioNodesToSkip()).isEmpty();
    }

    @Test
    public void shouldSkipUpToDateSongs() {

        String songPath = audioNode.getFile().getAbsolutePath();
        when(songRepository.findByPath(any())).thenReturn(songBuilder()
                .creationDate(LocalDateTime.now().plusDays(1))
                .updateDate(null)
                .path(songPath)
                .build());

        Plan plan = batchLibraryImportPlanner.plan(ImmutableList.of(audioNode));

        assertThat(plan.getAudioNodesToImport()).isEmpty();
        assertThat(plan.getAudioNodesToSkip()).containsExactly(audioNode);
    }
}