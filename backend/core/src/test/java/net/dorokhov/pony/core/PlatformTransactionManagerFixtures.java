package net.dorokhov.pony.core;

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
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus(true);
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
        }
    }
}
