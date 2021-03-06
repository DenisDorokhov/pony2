package net.dorokhov.pony.core.installation.service;

import net.dorokhov.pony.api.config.service.ConfigService;
import net.dorokhov.pony.api.installation.domain.Installation;
import net.dorokhov.pony.api.installation.service.InstallationService;
import net.dorokhov.pony.core.installation.repository.InstallationRepository;
import net.dorokhov.pony.api.installation.service.command.InstallationCommand;
import net.dorokhov.pony.api.installation.service.exception.AlreadyInstalledException;
import net.dorokhov.pony.api.installation.service.exception.NotInstalledException;
import net.dorokhov.pony.api.log.service.LogService;
import net.dorokhov.pony.api.user.domain.User.Role;
import net.dorokhov.pony.api.user.service.UserService;
import net.dorokhov.pony.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony.api.user.service.exception.DuplicateEmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Service
@CacheConfig(cacheNames = "pony.installation")
public class InstallationServiceImpl implements InstallationService {
    
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
    @Cacheable(key = CACHE_KEY)
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
    @CacheEvict(key = CACHE_KEY)
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
        } catch (DuplicateEmailException e) {
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
    @CacheEvict(key = CACHE_KEY)
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
