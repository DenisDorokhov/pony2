package net.dorokhov.pony.core.config.service;

import java.io.File;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.config.domain.Config;
import net.dorokhov.pony.core.config.repository.ConfigRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceImplTest {
    
    @InjectMocks
    private ConfigServiceImpl configService;
    
    @Mock
    private ConfigRepository configRepository;

    @Test
    public void shouldGetEnabledScanInterval() throws Exception {
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(Config.of(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        assertThat(configService.getAutoScanInterval()).isEqualTo(1000);
    }
    
    @Test
    public void shouldGetDisabledAutoScanInterval() throws Exception {
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(null);
        assertThat(configService.getAutoScanInterval()).isNull();
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(Config.of(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, null));
        assertThat(configService.getAutoScanInterval()).isNull();
    }

    @Test
    public void shouldSaveEnabledAutoScanInterval() throws Exception {
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(Config.of(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        configService.saveAutoScanInterval(2000);
        
        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("2000");
    }

    @Test
    public void shouldSaveDisabledAutoScanInterval() throws Exception {
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(Config.of(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        configService.saveAutoScanInterval(null);
        
        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isNull();
    }

    @Test
    public void shouldSaveAutoScanIntervalWhichDidNotExist() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(null);
        configService.saveAutoScanInterval(2000);

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("2000");
    }

    @Test
    public void shouldFetchExistingLibraryFolders() throws Exception {
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]"));
        assertThat(configService.getLibraryFolders()).containsExactly(new File("foo"), new File("bar"));
    }

    @Test
    public void shouldFetchNotExistingLibraryFolders() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, null));
        assertThat(configService.getLibraryFolders()).isEmpty();
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(null);
        assertThat(configService.getLibraryFolders()).isEmpty();
    }

    @Test
    public void shouldSaveExistingLibraryFolders() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]"));
        configService.saveLibraryFolders(ImmutableList.of(new File("foobar")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("[\"foobar\"]");
    }

    @Test
    public void shouldSaveEmptyLibraryFolders() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]"));
        configService.saveLibraryFolders(emptyList());

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("[]");
    }

    @Test
    public void shouldSaveLibraryFoldersWhichDidNotExist() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(null);
        configService.saveLibraryFolders(ImmutableList.of(new File("foobar")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("[\"foobar\"]");
    }
}
