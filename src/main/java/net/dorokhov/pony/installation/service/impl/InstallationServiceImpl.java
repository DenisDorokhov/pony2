package net.dorokhov.pony.installation.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.repository.InstallationRepository;
import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.installation.service.command.InstallationCommand;
import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.installation.service.exception.NotInstalledException;
import net.dorokhov.pony.log.service.LogService;
import net.dorokhov.pony.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

@Service
public class InstallationServiceImpl implements InstallationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InstallationRepository installationRepository;
    private final BuildVersionProvider buildVersionProvider;
    private final ConfigService configService;
    private final UserService userService;
    private final LogService logService;

    public InstallationServiceImpl(InstallationRepository installationRepository,
                                   BuildVersionProvider buildVersionProvider,
                                   ConfigService configService,
                                   UserService userService,
                                   LogService logService,
                                   PlatformTransactionManager transactionManager) {

        this.installationRepository = installationRepository;
        this.buildVersionProvider = buildVersionProvider;
        this.configService = configService;
        this.userService = userService;
        this.logService = logService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Installation> getInstallation() {
        Page<Installation> page = installationRepository.findAll(new PageRequest(0, 2));
        if (page.getTotalElements() > 1) {
            throw new IllegalStateException("More than one installations detected.");
        } else if (page.getTotalElements() == 0) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(page.getContent().get(0));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    synchronized public Installation install(InstallationCommand command) throws AlreadyInstalledException {
        getInstallation().ifPresent(installation -> {
            throw new AlreadyInstalledException();
        });

        configService.saveAutoScanInterval(command.getAutoScanInterval().orElse(null));
        configService.saveLibraryFolders(command.getLibraryFolders());

        userService.create(command.getUserCreationCommand());

        Installation installation = installationRepository.save(Installation.builder()
                .version(buildVersionProvider.getBuildVersion().getVersion())
                .build());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                logService.info(log, "installationService.installed", "The application has been installed.");
            }
        });
        
        return installation;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    synchronized public Optional<Installation> upgradeIfNeeded() throws NotInstalledException {
        
        final Installation currentInstallation = getInstallation().orElseThrow(NotInstalledException::new);
        String buildVersion = buildVersionProvider.getBuildVersion().getVersion();
        if (currentInstallation.getVersion().equals(buildVersion)) {
            return Optional.empty();
        }
            
        Installation upgradedInstallation = installationRepository.save(Installation.builder(currentInstallation)
                .version(buildVersion)
                .build());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                logService.info(log, "installationService.upgraded", ImmutableList.of(currentInstallation.getVersion(), buildVersion),
                        String.format("The application has been upgraded from '%s' to '%s'.", currentInstallation.getVersion(), buildVersion));
            }
        });
        
        return Optional.of(upgradedInstallation);
    }
}
