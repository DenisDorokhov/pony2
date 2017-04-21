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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceImplTests {
    
    @Mock
    private ConfigRepository configRepository;
    
    @InjectMocks
    private ConfigServiceImpl configService;

    @Test
    public void getEnabledScanInterval() throws Exception {
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        assertThat(configService.getAutoScanInterval()).hasValue(1000);
    }
    
    @Test
    public void getDisabledAutoScanInterval() throws Exception {
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(null);
        assertThat(configService.getAutoScanInterval()).isEmpty();
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, null));
        assertThat(configService.getAutoScanInterval()).isEmpty();
    }

    @Test
    public void saveEnabledAutoScanInterval() throws Exception {
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        configService.saveAutoScanInterval(2000);
        
        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        
        assertThat(savedConfig.getValue().getValue()).hasValue("2000");
    }

    @Test
    public void saveDisabledAutoScanInterval() throws Exception {
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        configService.saveAutoScanInterval(null);
        
        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        
        assertThat(savedConfig.getValue().getValue()).isEmpty();
    }

    @Test
    public void saveAutoScanIntervalWhichDidNotExist() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(null);
        configService.saveAutoScanInterval(2000);

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());

        assertThat(savedConfig.getValue().getValue()).hasValue("2000");
    }

    @Test
    public void fetchExistingLibraryFolders() throws Exception {
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(new Config(
                        ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, 
                        "foo" + ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS_SEPARATOR + "bar"));
        assertThat(configService.fetchLibraryFolders()).containsExactly(new File("foo"), new File("bar"));

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, null));
        assertThat(configService.fetchLibraryFolders()).isEmpty();
    }

    @Test
    public void fetchNotExistingLibraryFolders() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, null));
        assertThat(configService.fetchLibraryFolders()).isEmpty();
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(null);
        assertThat(configService.fetchLibraryFolders()).isEmpty();
    }

    @Test
    public void saveExistingLibraryFolders() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(new Config(
                        ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, 
                        "foo" + ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS_SEPARATOR + "bar"));
        configService.saveLibraryFolders(ImmutableList.of(new File("foobar" + ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS_SEPARATOR + "barfoo")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());

        assertThat(savedConfig.getValue().getValue()).hasValue("foobar" + ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS_SEPARATOR + "barfoo");
    }

    @Test
    public void saveNotExistingLibraryFolders() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(new Config(
                        ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, 
                        "foo" + ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS_SEPARATOR + "bar"));
        configService.saveLibraryFolders(ImmutableList.of());

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());

        assertThat(savedConfig.getValue().getValue()).isEmpty();
    }

    @Test
    public void saveLibraryFoldersWhichDidNotExist() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(null);
        configService.saveLibraryFolders(ImmutableList.of(new File("foobar")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());

        assertThat(savedConfig.getValue().getValue()).hasValue("foobar");
    }
}
