package net.dorokhov.pony2.core.config.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.config.domain.Config;
import net.dorokhov.pony2.api.config.domain.ConfigChangeEvent;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.config.repository.ConfigRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfigServiceImplTest {
    
    @InjectMocks
    private ConfigServiceImpl configService;
    
    @Mock
    private ConfigRepository configRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private LibraryJobSynchronizer jobSynchronizer;

    @ParameterizedTest
    @CsvSource({"true,false,true", "true,true,false", "false,false,false", "false,true,false"})
    public void shouldStopDiscoveryBeforeSavingDisabledConfig(boolean wasEnabled, boolean enabled, boolean cancel) {
        when(configRepository.findAll()).thenReturn(List.of(
                Config.of(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED, Boolean.toString(wasEnabled))));

        configService.update(new ConfigSetUpdateCommand()
                .setDiscoveryEnabled(enabled)
                .setLlmUrl("http://localhost/v1")
                .setLlmModel("model"));

        if (cancel) {
            InOrder order = inOrder(jobSynchronizer, configRepository);
            order.verify(jobSynchronizer).cancelDiscoveryAndWait(Duration.ofSeconds(30));
            order.verify(configRepository, times(5)).save(any());
        } else {
            verifyNoInteractions(jobSynchronizer);
        }
    }

    @Test
    public void shouldNotSaveConfigWhenDiscoveryCannotStop() {
        when(configRepository.findAll()).thenReturn(List.of(
                Config.of(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED, "true")));
        doThrow(new IllegalStateException("Cancellation timed out"))
                .when(jobSynchronizer).cancelDiscoveryAndWait(Duration.ofSeconds(30));

        assertThatThrownBy(() -> configService.update(new ConfigSetUpdateCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cancellation timed out");

        verify(configRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    public void shouldPublishOldAndNewConfigSnapshots() {
        Config model = Config.of(ConfigServiceImpl.CONFIG_LLM_MODEL, "old-model");
        when(configRepository.findAll()).thenReturn(List.of(model));
        when(configRepository.findById(any())).thenReturn(Optional.empty());
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_MODEL)).thenReturn(Optional.of(model));

        configService.update(new ConfigSetUpdateCommand()
                .setLlmUrl("http://localhost:11434/v1")
                .setLlmModel("new-model"));

        ArgumentCaptor<ConfigChangeEvent> event = ArgumentCaptor.forClass(ConfigChangeEvent.class);
        verify(applicationEventPublisher).publishEvent(event.capture());
        assertThat(model.getValue()).isEqualTo("new-model");
        assertThat(event.getValue().oldConfig().llmModel()).isEqualTo("old-model");
        assertThat(event.getValue().newConfig().llmModel()).isEqualTo("new-model");
    }

    @Test
    public void shouldFetchExistingConfigSet() {

        LocalDateTime creationDate = LocalDateTime.parse("2026-01-01T00:00:00");
        LocalDateTime updateDate = LocalDateTime.parse("2026-01-02T00:00:00");
        when(configRepository.findAll()).thenReturn(List.of(
                Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]")
                        .setCreationDate(creationDate),
                Config.of(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED, "true")
                        .setCreationDate(creationDate),
                Config.of(ConfigServiceImpl.CONFIG_LLM_URL, "http://localhost:11434/v1")
                        .setCreationDate(creationDate),
                Config.of(ConfigServiceImpl.CONFIG_LLM_MODEL, "qwen3.5-35b-a3b")
                        .setCreationDate(creationDate)
                        .setUpdateDate(updateDate),
                Config.of(ConfigServiceImpl.CONFIG_LLM_API_KEY, "secret")
                        .setCreationDate(creationDate)
        ));

        ConfigSet configSet = configService.get();

        assertThat(configSet.updateDate()).isEqualTo(updateDate);
        assertThat(configSet.libraryFolders()).containsExactly(new File("foo").getAbsoluteFile(), new File("bar").getAbsoluteFile());
        assertThat(configSet.llmUrl()).isEqualTo("http://localhost:11434/v1");
        assertThat(configSet.llmModel()).isEqualTo("qwen3.5-35b-a3b");
        assertThat(configSet.llmApiKey()).isEqualTo("secret");
        assertThat(configSet.discoveryEnabled()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"music", "./music", "other/../music/."})
    public void shouldNormalizeLibraryFoldersWhenReading(String path) {
        String originalValue = JsonConverter.toJson(List.of(path));
        Config folders = Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, originalValue);
        when(configRepository.findAll()).thenReturn(List.of(folders));

        ConfigSet configSet = configService.get();

        assertThat(configSet.libraryFolders()).containsExactly(new File("music").getAbsoluteFile());
        assertThat(folders.getValue()).isEqualTo(originalValue);
        verify(configRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"music", "./music", "other/../music/."})
    public void shouldNormalizeLibraryFoldersWhenSaving(String path) {
        Config folders = Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[]");
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS)).thenReturn(Optional.of(folders));

        configService.update(new ConfigSetUpdateCommand().setLibraryFolders(List.of(new File(path))));

        assertThat(JsonConverter.listFromJson(folders.getValue(), String.class))
                .containsExactly(new File("music").getAbsolutePath());
    }

    @EnabledOnOs(OS.WINDOWS)
    @ParameterizedTest
    @ValueSource(strings = {"M:", "M:\\", "M:\\\\"})
    public void shouldKeepWindowsLibraryFolderStableAfterSettingsSave(String path) {
        Config folders = Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, JsonConverter.toJson(List.of(path)));
        when(configRepository.findAll()).thenReturn(List.of(folders));
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS)).thenReturn(Optional.of(folders));
        File folderBeforeSave = configService.get().libraryFolders().getFirst();

        configService.update(new ConfigSetUpdateCommand()
                .setLibraryFolders(List.of(new File(folderBeforeSave.getAbsolutePath()))));

        File folderAfterSave = configService.get().libraryFolders().getFirst();
        assertThat(folderBeforeSave.isAbsolute()).isTrue();
        assertThat(folderAfterSave).isEqualTo(folderBeforeSave);
        assertThat(JsonConverter.listFromJson(folders.getValue(), String.class)).containsExactly(folderBeforeSave.getPath());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void shouldKeepAbsoluteLinuxLibraryFolderUnchanged() {
        String originalValue = "[\"/home/pony/music\"]";
        Config folders = Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, originalValue);
        when(configRepository.findAll()).thenReturn(List.of(folders));
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS)).thenReturn(Optional.of(folders));
        ConfigSet configSet = configService.get();

        configService.update(new ConfigSetUpdateCommand().setLibraryFolders(configSet.libraryFolders()));

        assertThat(configSet.libraryFolders()).containsExactly(new File("/home/pony/music"));
        assertThat(folders.getValue()).isEqualTo(originalValue);
    }

    @Test
    public void shouldFetchEmptyConfigSet() {

        when(configRepository.findAll()).thenReturn(List.of(
                Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, null),
                Config.of(ConfigServiceImpl.CONFIG_LLM_URL, null),
                Config.of(ConfigServiceImpl.CONFIG_LLM_MODEL, null),
                Config.of(ConfigServiceImpl.CONFIG_LLM_API_KEY, null)
        ));

        ConfigSet configSet = configService.get();

        assertThat(configSet.updateDate()).isNull();
        assertThat(configSet.libraryFolders()).isEmpty();
        assertThat(configSet.llmUrl()).isNull();
        assertThat(configSet.llmModel()).isNull();
        assertThat(configSet.llmApiKey()).isNull();
        assertThat(configSet.discoveryEnabled()).isFalse();
    }

    @Test
    public void shouldFetchConfigSetWhenConfigDoesNotExist() {

        when(configRepository.findAll()).thenReturn(emptyList());

        ConfigSet configSet = configService.get();

        assertThat(configSet.updateDate()).isNull();
        assertThat(configSet.libraryFolders()).isEmpty();
        assertThat(configSet.llmUrl()).isNull();
        assertThat(configSet.llmModel()).isNull();
        assertThat(configSet.llmApiKey()).isNull();
        assertThat(configSet.discoveryEnabled()).isFalse();
    }

    @Test
    public void shouldUpdateConfig() {

        when(configRepository.findById(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED))
                .thenReturn(Optional.empty());
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]")));
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_URL))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LLM_URL, "http://localhost:11434")));
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_MODEL))
                .thenReturn(Optional.empty());
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_API_KEY))
                .thenReturn(Optional.empty());

        configService.update(new ConfigSetUpdateCommand()
                .setLibraryFolders(ImmutableList.of(new File("foobar")))
                .setDiscoveryEnabled(true)
                .setLlmUrl("http://localhost:11434/v1")
                .setLlmModel("qwen3.5-35b-a3b")
                .setLlmApiKey("secret"));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository, times(5)).save(savedConfig.capture());
        assertThat(savedConfig.getAllValues()).satisfiesExactly(
                libraryFolders -> {
                    assertThat(libraryFolders.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS);
                    assertThat(JsonConverter.listFromJson(libraryFolders.getValue(), String.class))
                            .containsExactly(new File("foobar").getAbsolutePath());
                },
                discoveryEnabled -> {
                    assertThat(discoveryEnabled.getId()).isEqualTo(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED);
                    assertThat(discoveryEnabled.getValue()).isEqualTo("true");
                },
                llmUrl -> {
                    assertThat(llmUrl.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LLM_URL);
                    assertThat(llmUrl.getValue()).isEqualTo("http://localhost:11434/v1");
                },
                llmModel -> {
                    assertThat(llmModel.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LLM_MODEL);
                    assertThat(llmModel.getValue()).isEqualTo("qwen3.5-35b-a3b");
                },
                llmApiKey -> {
                    assertThat(llmApiKey.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LLM_API_KEY);
                    assertThat(llmApiKey.getValue()).isEqualTo("secret");
                });
    }

    @Test
    public void shouldUpdateEmptyConfigValues() {

        when(configRepository.findById(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED))
                .thenReturn(Optional.empty());
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]")));
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_URL))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LLM_URL, "http://localhost:11434")));
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_MODEL))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LLM_MODEL, "qwen3.5-35b-a3b")));
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_API_KEY))
                .thenReturn(Optional.of(Config.of(ConfigServiceImpl.CONFIG_LLM_API_KEY, "secret")));

        configService.update(new ConfigSetUpdateCommand()
                .setLibraryFolders(emptyList())
                .setLlmUrl("")
                .setLlmModel(" ")
                .setLlmApiKey(null));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository, times(5)).save(savedConfig.capture());
        assertThat(savedConfig.getAllValues()).satisfiesExactly(
                libraryFolders -> {
                    assertThat(libraryFolders.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS);
                    assertThat(libraryFolders.getValue()).isEqualTo("[]");
                },
                discoveryEnabled -> {
                    assertThat(discoveryEnabled.getId()).isEqualTo(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED);
                    assertThat(discoveryEnabled.getValue()).isEqualTo("false");
                },
                llmUrl -> {
                    assertThat(llmUrl.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LLM_URL);
                    assertThat(llmUrl.getValue()).isNull();
                },
                llmModel -> {
                    assertThat(llmModel.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LLM_MODEL);
                    assertThat(llmModel.getValue()).isNull();
                },
                llmApiKey -> {
                    assertThat(llmApiKey.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LLM_API_KEY);
                    assertThat(llmApiKey.getValue()).isNull();
                });
    }

    @Test
    public void shouldUpdateConfigWhichDidNotExist() {

        when(configRepository.findById(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED))
                .thenReturn(Optional.empty());
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS))
                .thenReturn(Optional.empty());
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_URL))
                .thenReturn(Optional.empty());
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_MODEL))
                .thenReturn(Optional.empty());
        when(configRepository.findById(ConfigServiceImpl.CONFIG_LLM_API_KEY))
                .thenReturn(Optional.empty());

        configService.update(new ConfigSetUpdateCommand()
                .setLibraryFolders(ImmutableList.of(new File("foobar"))));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository, times(5)).save(savedConfig.capture());
        assertThat(savedConfig.getAllValues()).satisfiesExactly(
                libraryFolders -> {
                    assertThat(libraryFolders.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS);
                    assertThat(JsonConverter.listFromJson(libraryFolders.getValue(), String.class))
                            .containsExactly(new File("foobar").getAbsolutePath());
                },
                discoveryEnabled -> {
                    assertThat(discoveryEnabled.getId()).isEqualTo(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED);
                    assertThat(discoveryEnabled.getValue()).isEqualTo("false");
                },
                llmUrl -> {
                    assertThat(llmUrl.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LLM_URL);
                    assertThat(llmUrl.getValue()).isNull();
                },
                llmModel -> {
                    assertThat(llmModel.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LLM_MODEL);
                    assertThat(llmModel.getValue()).isNull();
                },
                llmApiKey -> {
                    assertThat(llmApiKey.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LLM_API_KEY);
                    assertThat(llmApiKey.getValue()).isNull();
                });
    }

    @Test
    public void shouldRejectLlmUrlWithoutLlmModel() {

        assertThatThrownBy(() -> configService.update(new ConfigSetUpdateCommand()
                .setDiscoveryEnabled(true)
                .setLibraryFolders(emptyList())
                .setLlmUrl("http://localhost:11434/v1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LLM model must be configured when discovery is enabled.");

        verify(configRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    public void shouldRejectLlmModelWithoutLlmUrl() {

        assertThatThrownBy(() -> configService.update(new ConfigSetUpdateCommand()
                .setDiscoveryEnabled(true)
                .setLibraryFolders(emptyList())
                .setLlmModel("qwen3.5-35b-a3b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LLM URL must be configured when discovery is enabled.");

        verify(configRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    public void shouldRejectLlmApiKeyWithoutLlmUrl() {

        assertThatThrownBy(() -> configService.update(new ConfigSetUpdateCommand()
                .setDiscoveryEnabled(true)
                .setLibraryFolders(emptyList())
                .setLlmApiKey("secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LLM URL must be configured when discovery is enabled.");

        verify(configRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    public void shouldLeaveDiscoveryDisabledWhenFlagIsMissing() {
        when(configRepository.findAll()).thenReturn(List.of(
                Config.of(ConfigServiceImpl.CONFIG_LLM_URL, "http://localhost:11434/v1"),
                Config.of(ConfigServiceImpl.CONFIG_LLM_MODEL, "model")
        ));

        assertThat(configService.get().discoveryEnabled()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "http://localhost", "https://localhost/", "ftp://localhost/v1", "http:///v1"})
    public void shouldRejectInvalidUrlWhenDiscoveryIsEnabled(String url) {
        assertThatThrownBy(() -> configService.update(new ConfigSetUpdateCommand()
                .setDiscoveryEnabled(true)
                .setLlmUrl(url)
                .setLlmModel("model")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LLM URL must be an absolute HTTP(S) URL including an API path.");

        verifyNoInteractions(configRepository, applicationEventPublisher);
    }

    @Test
    public void shouldSaveIncompleteLlmConfigWhenDiscoveryIsDisabled() {
        configService.update(new ConfigSetUpdateCommand()
                .setLlmUrl("unfinished URL")
                .setLlmApiKey("secret"));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository, times(5)).save(savedConfig.capture());
        assertThat(savedConfig.getAllValues()).filteredOn(config -> config.getId().equals(ConfigServiceImpl.CONFIG_DISCOVERY_ENABLED))
                .singleElement().satisfies(config -> assertThat(config.getValue()).isEqualTo("false"));
        assertThat(savedConfig.getAllValues()).filteredOn(config -> config.getId().equals(ConfigServiceImpl.CONFIG_LLM_URL))
                .singleElement().satisfies(config -> assertThat(config.getValue()).isEqualTo("unfinished URL"));
        assertThat(savedConfig.getAllValues()).filteredOn(config -> config.getId().equals(ConfigServiceImpl.CONFIG_LLM_API_KEY))
                .singleElement().satisfies(config -> assertThat(config.getValue()).isEqualTo("secret"));
    }
}
