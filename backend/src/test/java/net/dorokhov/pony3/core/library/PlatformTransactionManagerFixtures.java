package net.dorokhov.pony3.core.library;

import jakarta.annotation.Nonnull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

public final class PlatformTransactionManagerFixtures {

    private PlatformTransactionManagerFixtures() {
    }
    
    public static PlatformTransactionManager transactionManager() {
        return new PlatformTransactionManagerImpl();
    }
    
    private static class PlatformTransactionManagerImpl implements PlatformTransactionManager {

        @Override
        public @Nonnull TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus(true);
        }

        @Override
        public void commit(@Nonnull TransactionStatus status) throws TransactionException {
        }

        @Override
        public void rollback(@Nonnull TransactionStatus status) throws TransactionException {
        }
    }
}
