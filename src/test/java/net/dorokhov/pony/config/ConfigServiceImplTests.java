package net.dorokhov.pony.config;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.entity.Config;
import net.dorokhov.pony.repository.ConfigRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceImplTests {
    
    @Mock
    private ConfigRepository configRepository;
    
    @InjectMocks
    private ConfigServiceImpl configService;

    @Test
    public void shouldGetEnabledScanInterval() throws Exception {
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        assertThat(configService.getAutoScanInterval()).isEqualTo(Optional.of(1000));
    }
    
    @Test
    public void shouldGetDisabledAutoScanInterval() throws Exception {
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(null);
        assertThat(configService.getAutoScanInterval()).isEqualTo(Optional.empty());
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL));
        assertThat(configService.getAutoScanInterval()).isEqualTo(Optional.empty());
    }

    @Test
    public void shouldSaveEnabledAutoScanInterval() throws Exception {
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        configService.saveAutoScanInterval(2000);
        
        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        
        assertThat(savedConfig.getValue().getValue()).isEqualTo(Optional.of("2000"));
    }

    @Test
    public void shouldSaveDisabledAutoScanInterval() throws Exception {
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        configService.saveAutoScanInterval(null);
        
        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        
        assertThat(savedConfig.getValue().getValue()).isEqualTo(Optional.empty());
    }

    @Test
    public void shouldSaveAutoScanIntervalWhichDidNotExist() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .thenReturn(null);
        configService.saveAutoScanInterval(2000);

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());

        assertThat(savedConfig.getValue().getValue()).isEqualTo(Optional.of("2000"));
    }

    @Test
    public void shouldFetchExistingLibraryFolders() throws Exception {
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(new Config(
                        ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, 
                        "foo" + ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS_SEPARATOR + "bar"));
        assertThat(configService.fetchLibraryFolders()).containsExactly(new File("foo"), new File("bar"));

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(new Config(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS));
        assertThat(configService.fetchLibraryFolders()).isEmpty();
    }

    @Test
    public void shouldFetchNotExistingLibraryFolders() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(new Config(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS));
        assertThat(configService.fetchLibraryFolders()).isEmpty();
        
        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(null);
        assertThat(configService.fetchLibraryFolders()).isEmpty();
    }

    @Test
    public void shouldSaveExistingLibraryFolders() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(new Config(
                        ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, 
                        "foo" + ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS_SEPARATOR + "bar"));
        configService.saveLibraryFolders(ImmutableList.of(new File("foobar")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());

        assertThat(savedConfig.getValue().getValue()).isEqualTo(Optional.of("foobar"));
    }

    @Test
    public void shouldSaveNotExistingLibraryFolders() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(new Config(
                        ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, 
                        "foo" + ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS_SEPARATOR + "bar"));
        configService.saveLibraryFolders(ImmutableList.of());

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());

        assertThat(savedConfig.getValue().getValue()).isEqualTo(Optional.empty());
    }

    @Test
    public void shouldSaveLibraryFoldersWhichDidNotExist() throws Exception {

        when(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(null);
        configService.saveLibraryFolders(ImmutableList.of(new File("foobar")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());

        assertThat(savedConfig.getValue().getValue()).isEqualTo(Optional.of("foobar"));
    }
}
