package net.dorokhov.pony2.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ShutdownService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean shutdownSignal = new AtomicBoolean(false);

    @EventListener({ContextClosedEvent.class})
    public void onApplicationEvent() {
        shutdownSignal.set(true);
        logger.info("Shutdown signal received.");
    }

    public boolean isShutdown() {
        return shutdownSignal.get();
    }
}
