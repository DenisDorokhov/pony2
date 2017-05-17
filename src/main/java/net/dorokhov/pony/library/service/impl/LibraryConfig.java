package net.dorokhov.pony.library.service.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class LibraryConfig {
    
    public static final String SCAN_JOB_EXECUTOR = "scanJobExecutor";
    public static final String SCAN_EXECUTOR = "scanExecutor";

    @Bean(SCAN_JOB_EXECUTOR)
    public ThreadPoolTaskExecutor scanJobExecutor() {
        return buildThreadPoolExecutor(2, SCAN_JOB_EXECUTOR);
    }

    @Bean(SCAN_EXECUTOR)
    public ThreadPoolTaskExecutor scanExecutor() {
        return buildThreadPoolExecutor(10, SCAN_EXECUTOR);
    }

    private ThreadPoolTaskExecutor buildThreadPoolExecutor(int maxPoolSize, String beanName) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(maxPoolSize);
        taskExecutor.setThreadNamePrefix(beanName + "-");
        return taskExecutor;
    }
}
