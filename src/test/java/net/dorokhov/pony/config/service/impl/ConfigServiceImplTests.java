package net.dorokhov.pony.config.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.config.domain.Config;
import net.dorokhov.pony.config.repository.ConfigRepository;
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
    
    @InjectMocks
    private ConfigServiceImpl configService;
    
    @Mock
    private ConfigRepository configRepository;

    @Test
    public void shouldGetEnabledScanInterval() throws Exception {
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        assertThat(configService.getAutoScanInterval()).isEqualTo(1000);
    }
    
    @Test
    public void shouldGetDisabledAutoScanInterval() throws Exception {
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(null);
        assertThat(configService.getAutoScanInterval()).isNull();
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, null));
        assertThat(configService.getAutoScanInterval()).isNull();
    }

    @Test
    public void shouldSaveEnabledAutoScanInterval() throws Exception {
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        configService.saveAutoScanInterval(2000);
        
        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("2000");
    }

    @Test
    public void shouldSaveDisabledAutoScanInterval() throws Exception {
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL, "1000"));
        configService.saveAutoScanInterval(null);
        
        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isNull();
    }

    @Test
    public void shouldSaveAutoScanIntervalWhichDidNotExist() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_AUTO_SCAN_INTERVAL))
                .willReturn(null);
        configService.saveAutoScanInterval(2000);

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("2000");
    }

    @Test
    public void shouldFetchExistingLibraryFolders() throws Exception {
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]"));
        assertThat(configService.getLibraryFolders()).containsExactly(new File("foo"), new File("bar"));
    }

    @Test
    public void shouldFetchNotExistingLibraryFolders() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, null));
        assertThat(configService.getLibraryFolders()).isEmpty();
        
        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(null);
        assertThat(configService.getLibraryFolders()).isEmpty();
    }

    @Test
    public void shouldSaveExistingLibraryFolders() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]"));
        configService.saveLibraryFolders(ImmutableList.of(new File("foobar")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("[\"foobar\"]");
    }

    @Test
    public void shouldSaveEmptyLibraryFolders() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(new Config(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]"));
        configService.saveLibraryFolders(ImmutableList.of());

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("[]");
    }

    @Test
    public void shouldSaveLibraryFoldersWhichDidNotExist() throws Exception {

        given(configRepository.findOne(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .willReturn(null);
        configService.saveLibraryFolders(ImmutableList.of(new File("foobar")));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository).save(savedConfig.capture());
        assertThat(savedConfig.getValue().getValue()).isEqualTo("[\"foobar\"]");
    }
}
