package net.dorokhov.pony3.core.installation.service;

import com.google.common.collect.Sets;
import net.dorokhov.pony3.api.config.service.ConfigService;
import net.dorokhov.pony3.api.installation.domain.Installation;
import net.dorokhov.pony3.api.installation.service.InstallationService;
import net.dorokhov.pony3.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony3.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony3.api.installation.service.exception.NotInstalledException;
import net.dorokhov.pony3.api.log.service.LogService;
import net.dorokhov.pony3.api.user.domain.User.Role;
import net.dorokhov.pony3.api.user.service.UserService;
import net.dorokhov.pony3.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony3.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony3.core.installation.repository.InstallationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.Optional;

import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
public class InstallationServiceImpl implements InstallationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InstallationRepository installationRepository;
    private final BuildVersionProvider buildVersionProvider;
    private final ConfigService configService;
    private final UserService userService;
    private final LogService logService;

    public InstallationServiceImpl(
            InstallationRepository installationRepository,
            BuildVersionProvider buildVersionProvider,
            ConfigService configService,
            UserService userService,
            LogService logService
    ) {
        this.installationRepository = installationRepository;
        this.buildVersionProvider = buildVersionProvider;
        this.configService = configService;
        this.userService = userService;
        this.logService = logService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Installation> getInstallation() {
        Page<Installation> page = installationRepository.findAll(PageRequest.of(0, 2));
        if (page.getTotalElements() > 1) {
            throw new IllegalStateException("More than one installations detected.");
        } else if (page.getTotalElements() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(page.getContent().getFirst());
        }
    }

    @Override
    @Transactional
    synchronized public Installation install(InstallationCommand command) throws AlreadyInstalledException {

        if (getInstallation().isPresent()) {
            throw new AlreadyInstalledException();
        }

        configService.saveAutoScanInterval(command.getAutoScanInterval());
        configService.saveLibraryFolders(command.getLibraryFolders());

        try {
            userService.create(new UserCreationCommand()
                    .setName(command.getAdminName())
                    .setEmail(command.getAdminEmail())
                    .setPassword(command.getAdminPassword())
                    .setRoles(Sets.newHashSet(Role.USER, Role.ADMIN))
            );
        } catch (DuplicateEmailException e) {
            throw new IllegalStateException(String.format("User '%s' already exists. Installation inconsistency?", command.getAdminEmail()), e);
        }

        Installation installation = installationRepository.save(new Installation()
                .setVersion(buildVersionProvider.getBuildVersion().getVersion())
        );

        registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                logService.info(logger, "The application has been installed.");
            }
        });

        return installation;
    }

    @Override
    @Transactional
    synchronized public Installation upgradeIfNeeded() throws NotInstalledException {

        Installation currentInstallation = getInstallation().orElse(null);
        if (currentInstallation == null) {
            throw new NotInstalledException();
        }

        String currentVersion = currentInstallation.getVersion();
        String buildVersion = buildVersionProvider.getBuildVersion().getVersion();
        if (currentInstallation.getVersion().equals(buildVersion)) {
            return currentInstallation;
        }

        Installation upgradedInstallation = installationRepository.save(currentInstallation
                .setVersion(buildVersion));

        registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                logService.info(logger, "The application has been upgraded from '{}' to '{}'.", currentVersion, buildVersion);
            }
        });

        return upgradedInstallation;
    }
}
