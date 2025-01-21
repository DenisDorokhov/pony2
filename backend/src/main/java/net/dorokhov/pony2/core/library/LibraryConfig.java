package net.dorokhov.pony2.core.library;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class LibraryConfig {
    
    public static final String SCAN_JOB_EXECUTOR = "scanJobExecutor";
    public static final String LIBRARY_IMPORT_EXECUTOR = "libraryImportExecutor";
    public static final String LIBRARY_SEARCH_INDEX_REBUILD_EXECUTOR = "librarySearchIndexRebuildExecutor";

    private final int importThreadPoolSize;
    
    public LibraryConfig(@Value("${pony.scan.importThreadPoolSize}") int importThreadPoolSize) {
        this.importThreadPoolSize = importThreadPoolSize;
    }

    @Bean(SCAN_JOB_EXECUTOR)
    public ThreadPoolTaskExecutor scanJobExecutor() {
        return buildThreadPoolExecutor(SCAN_JOB_EXECUTOR, 1);
    }

    @Bean(LIBRARY_IMPORT_EXECUTOR)
    public ThreadPoolTaskExecutor libraryImportExecutor() {
        return buildThreadPoolExecutor(LIBRARY_IMPORT_EXECUTOR, importThreadPoolSize);
    }

    @Bean(LIBRARY_SEARCH_INDEX_REBUILD_EXECUTOR)
    public ThreadPoolTaskExecutor librarySearchIndexRebuildExecutor() {
        return buildThreadPoolExecutor(LIBRARY_SEARCH_INDEX_REBUILD_EXECUTOR, 1);
    }

    private ThreadPoolTaskExecutor buildThreadPoolExecutor(String beanName, int poolSize) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(poolSize);
        taskExecutor.setMaxPoolSize(poolSize);
        taskExecutor.setThreadNamePrefix(beanName + "-");
        return taskExecutor;
    }
}
