package net.dorokhov.pony2.core.config.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.config.domain.Config;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.core.config.repository.ConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfigServiceImplTest {
    
    @InjectMocks
    private ConfigServiceImpl configService;
    
    @Mock
    private ConfigRepository configRepository;

    @Test
    public void shouldFetchExistingConfigSet() {

        LocalDateTime creationDate = LocalDateTime.parse("2026-01-01T00:00:00");
        LocalDateTime updateDate = LocalDateTime.parse("2026-01-02T00:00:00");
        when(configRepository.findAll()).thenReturn(List.of(
                Config.of(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS, "[\"foo\",\"bar\"]")
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
        assertThat(configSet.libraryFolders()).containsExactly(new File("foo"), new File("bar"));
        assertThat(configSet.llmUrl()).isEqualTo("http://localhost:11434/v1");
        assertThat(configSet.llmModel()).isEqualTo("qwen3.5-35b-a3b");
        assertThat(configSet.llmApiKey()).isEqualTo("secret");
        assertThat(configSet.llmEnabled()).isTrue();
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
        assertThat(configSet.llmEnabled()).isFalse();
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
        assertThat(configSet.llmEnabled()).isFalse();
    }

    @Test
    public void shouldUpdateConfig() {

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
                .setLlmUrl("http://localhost:11434/v1")
                .setLlmModel("qwen3.5-35b-a3b")
                .setLlmApiKey("secret"));

        ArgumentCaptor<Config> savedConfig = ArgumentCaptor.forClass(Config.class);
        verify(configRepository, times(4)).save(savedConfig.capture());
        assertThat(savedConfig.getAllValues()).satisfiesExactly(
                libraryFolders -> {
                    assertThat(libraryFolders.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS);
                    assertThat(libraryFolders.getValue()).isEqualTo("[\"foobar\"]");
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
        verify(configRepository, times(4)).save(savedConfig.capture());
        assertThat(savedConfig.getAllValues()).satisfiesExactly(
                libraryFolders -> {
                    assertThat(libraryFolders.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS);
                    assertThat(libraryFolders.getValue()).isEqualTo("[]");
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
        verify(configRepository, times(4)).save(savedConfig.capture());
        assertThat(savedConfig.getAllValues()).satisfiesExactly(
                libraryFolders -> {
                    assertThat(libraryFolders.getId()).isEqualTo(ConfigServiceImpl.CONFIG_LIBRARY_FOLDERS);
                    assertThat(libraryFolders.getValue()).isEqualTo("[\"foobar\"]");
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
                .setLibraryFolders(emptyList())
                .setLlmUrl("http://localhost:11434/v1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LLM model must be configured when LLM URL is configured.");

        verify(configRepository, never()).save(any());
    }

    @Test
    public void shouldRejectLlmModelWithoutLlmUrl() {

        assertThatThrownBy(() -> configService.update(new ConfigSetUpdateCommand()
                .setLibraryFolders(emptyList())
                .setLlmModel("qwen3.5-35b-a3b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LLM URL must be configured when LLM model is configured.");

        verify(configRepository, never()).save(any());
    }

    @Test
    public void shouldRejectLlmApiKeyWithoutLlmUrl() {

        assertThatThrownBy(() -> configService.update(new ConfigSetUpdateCommand()
                .setLibraryFolders(emptyList())
                .setLlmApiKey("secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LLM URL must be configured when LLM API key is configured.");

        verify(configRepository, never()).save(any());
    }
}
