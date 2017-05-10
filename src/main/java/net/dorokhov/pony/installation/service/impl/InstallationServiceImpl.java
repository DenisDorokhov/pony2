package net.dorokhov.pony.installation.service.impl;

import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.repository.InstallationRepository;
import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.installation.service.command.InstallationCommand;
import net.dorokhov.pony.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.installation.service.exception.NotInstalledException;
import net.dorokhov.pony.log.service.LogService;
import net.dorokhov.pony.user.UserService;
import net.dorokhov.pony.user.domain.User.Role;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.service.exception.UserExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
public class InstallationServiceImpl implements InstallationService {
    
    private static final String CACHE_NAME = "pony.installation";
    private static final String CACHE_KEY = "'currentInstallation'";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InstallationRepository installationRepository;
    private final BuildVersionProvider buildVersionProvider;
    private final ConfigService configService;
    private final UserService userService;
    private final LogService logService;

    public InstallationServiceImpl(InstallationRepository installationRepository,
                                   BuildVersionProvider buildVersionProvider,
                                   ConfigService configService,
                                   UserService userService,
                                   LogService logService) {
        this.installationRepository = installationRepository;
        this.buildVersionProvider = buildVersionProvider;
        this.configService = configService;
        this.userService = userService;
        this.logService = logService;
    }

    @Override
    @Cacheable(cacheNames = CACHE_NAME, key = CACHE_KEY)
    @Transactional(readOnly = true)
    public Installation getInstallation() {
        Page<Installation> page = installationRepository.findAll(new PageRequest(0, 2));
        if (page.getTotalElements() > 1) {
            throw new IllegalStateException("More than one installations detected.");
        } else if (page.getTotalElements() == 0) {
            return null;
        } else {
            return page.getContent().get(0);
        }
    }

    @Override
    @CachePut(cacheNames = CACHE_NAME, key = CACHE_KEY)
    @Transactional
    synchronized public Installation install(InstallationCommand command) throws AlreadyInstalledException {
        
        if (getInstallation() != null) {
            throw new AlreadyInstalledException();
        }

        configService.saveAutoScanInterval(command.getAutoScanInterval());
        configService.saveLibraryFolders(command.getLibraryFolders());

        try {
            userService.create(UserCreationCommand.builder()
                    .name(command.getAdminName())
                    .email(command.getAdminEmail())
                    .password(command.getAdminPassword())
                    .roles(Role.USER, Role.ADMIN)
                    .build());
        } catch (UserExistsException e) {
            throw new IllegalStateException(String.format("User '%s' already exists. Installation inconsistency?", command.getAdminEmail()), e);
        }

        Installation installation = installationRepository.save(Installation.builder()
                .version(buildVersionProvider.getBuildVersion().getVersion())
                .build());

        registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                logService.info(logger,"The application has been installed.");
            }
        });
        
        return installation;
    }

    @Override
    @CachePut(cacheNames = CACHE_NAME, key = CACHE_KEY)
    @Transactional
    synchronized public Installation upgradeIfNeeded() throws NotInstalledException {
        
        final Installation currentInstallation = getInstallation();
        if (currentInstallation == null) {
            throw new NotInstalledException();
        }
        
        String buildVersion = buildVersionProvider.getBuildVersion().getVersion();
        if (currentInstallation.getVersion().equals(buildVersion)) {
            return currentInstallation;
        }
            
        Installation upgradedInstallation = installationRepository.save(Installation.builder(currentInstallation)
                .version(buildVersion)
                .build());

        registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                logService.info(logger, "The application has been upgraded from '{}' to '{}'.", currentInstallation.getVersion(), buildVersion);
            }
        });
        
        return upgradedInstallation;
    }
}
