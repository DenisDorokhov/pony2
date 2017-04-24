package net.dorokhov.pony.installation;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.build.BuildVersionProvider;
import net.dorokhov.pony.config.ConfigService;
import net.dorokhov.pony.entity.Installation;
import net.dorokhov.pony.installation.domain.InstallationDraft;
import net.dorokhov.pony.installation.exception.AlreadyInstalledException;
import net.dorokhov.pony.installation.exception.NotInstalledException;
import net.dorokhov.pony.logging.LogService;
import net.dorokhov.pony.repository.InstallationRepository;
import net.dorokhov.pony.user.TokenSecretManager;
import net.dorokhov.pony.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Optional;

@Service
public class InstallationServiceImpl implements InstallationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InstallationRepository installationRepository;
    private final BuildVersionProvider buildVersionProvider;
    private final TokenSecretManager tokenSecretManager;
    private final ConfigService configService;
    private final UserService userService;
    private final LogService logService;

    private final TransactionTemplate transactionTemplate;

    public InstallationServiceImpl(InstallationRepository installationRepository,
                                   BuildVersionProvider buildVersionProvider,
                                   TokenSecretManager tokenSecretManager,
                                   ConfigService configService,
                                   UserService userService,
                                   LogService logService,
                                   PlatformTransactionManager transactionManager) {

        this.installationRepository = installationRepository;
        this.tokenSecretManager = tokenSecretManager;
        this.configService = configService;
        this.userService = userService;
        this.logService = logService;

        transactionTemplate = new TransactionTemplate(transactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        this.buildVersionProvider = buildVersionProvider;
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
    synchronized public Installation install(InstallationDraft draft) throws AlreadyInstalledException {
        getInstallation().ifPresent(installation -> {
            throw new AlreadyInstalledException();
        });

        try {
            tokenSecretManager.generateAndStoreTokenSecret();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        configService.saveAutoScanInterval(draft.getAutoScanInterval().orElse(null));
        configService.saveLibraryFolders(draft.getLibraryFolders());

        userService.create(draft.getUserCreationDraft());

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
