package net.dorokhov.pony.installation.service.command;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.user.service.command.UserCreationCommand;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class InstallationCommand {

    private Integer autoScanInterval;
    private List<File> libraryFolders;
    private UserCreationCommand userCreationCommand;

    public InstallationCommand(Integer autoScanInterval, List<File> libraryFolders, UserCreationCommand userCreationCommand) {
        this.autoScanInterval = autoScanInterval;
        this.libraryFolders = ImmutableList.copyOf(checkNotNull(libraryFolders));
        this.userCreationCommand = checkNotNull(userCreationCommand);
    }

    public Optional<Integer> getAutoScanInterval() {
        return Optional.ofNullable(autoScanInterval);
    }

    public List<File> getLibraryFolders() {
        return libraryFolders;
    }

    public UserCreationCommand getUserCreationCommand() {
        return userCreationCommand;
    }
}
