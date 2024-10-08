package net.dorokhov.pony2.core.installation.service;

import com.google.common.collect.Sets;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.installation.domain.Installation;
import net.dorokhov.pony2.api.installation.service.InstallationService;
import net.dorokhov.pony2.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony2.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony2.api.installation.service.exception.NotInstalledException;
import net.dorokhov.pony2.api.library.service.ScanJobService;
import net.dorokhov.pony2.api.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.api.user.domain.User.Role;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony2.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony2.core.installation.repository.InstallationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
public class InstallationServiceImpl implements InstallationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InstallationRepository installationRepository;
    private final BuildVersionProvider buildVersionProvider;
    private final ConfigService configService;
    private final UserService userService;
    private final LogService logService;
    private final ScanJobService scanJobService;

    private final TransactionTemplate transactionTemplate;

    public InstallationServiceImpl(
            InstallationRepository installationRepository,
            BuildVersionProvider buildVersionProvider,
            ConfigService configService,
            UserService userService,
            LogService logService,
            ScanJobService scanJobService,
            PlatformTransactionManager transactionManager
    ) {
        this.installationRepository = installationRepository;
        this.buildVersionProvider = buildVersionProvider;
        this.configService = configService;
        this.userService = userService;
        this.logService = logService;
        this.scanJobService = scanJobService;

        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
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
                transactionTemplate.executeWithoutResult(status ->
                        logService.info(logger, "The application has been installed."));
                if (command.isStartScanJobAfterInstallation()) {
                    try {
                        transactionTemplate.executeWithoutResult(status -> {
                            try {
                                scanJobService.startScanJob();
                            } catch (ConcurrentScanException e) {
                                logService.error(logger, "Could not start scan job after installation: scan job is already running.");
                            }
                        });
                    } catch (Exception e) {
                        logger.error("Could not start scan job due to unexpected error.", e);
                    }
                }
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
                transactionTemplate.executeWithoutResult(status ->
                        logService.info(logger, "The application has been upgraded from '{}' to '{}'.", currentVersion, buildVersion));
            }
        });

        return upgradedInstallation;
    }
}
