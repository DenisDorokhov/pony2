package net.dorokhov.pony3.core.config.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony3.api.config.domain.Config;
import net.dorokhov.pony3.core.config.repository.ConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfigServiceImplTest {
    
    @InjectMocks
    private ConfigServiceImpl configService;
    
    @Mock
    private ConfigRepository configRepository;

    @Test
    public void shouldFetchExistingLibraryFolders() {

        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]")));

        assertThat(configService.getLibraryFolders()).containsExactly(new File("foo"), new File("bar"));
    }

    @Test
    public void shouldFetchNotExistingLibraryFolders() {

        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, null)));
        assertThat(configService.getLibraryFolders()).isEmpty();
        
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Optional.empty());
        assertThat(configService.getLibraryFolders()).isEmpty();
    }

    @Test
    public void shouldSaveExistingLibraryFolders() {

        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]")));

        configService.saveLibraryFolders(ImmutableList.of(new File("foobar")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("[\"foobar\"]");
    }

    @Test
    public void shouldSaveEmptyLibraryFolders() {

        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]")));

        configService.saveLibraryFolders(emptyList());

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("[]");
    }

    @Test
    public void shouldSaveLibraryFoldersWhichDidNotExist() {

        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Optional.empty());

        configService.saveLibraryFolders(ImmutableList.of(new File("foobar")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("[\"foobar\"]");
    }
}
