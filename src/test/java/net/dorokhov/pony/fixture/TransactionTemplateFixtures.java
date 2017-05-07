package net.dorokhov.pony.fixture;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public final class TransactionTemplateFixtures {
    
    private TransactionTemplateFixtures() {
    }

    public static TransactionTemplate get() {
        PlatformTransactionManager platformTransactionManager = mock(PlatformTransactionManager.class);
        given(platformTransactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
        return new TransactionTemplate(platformTransactionManager);
    }
}
