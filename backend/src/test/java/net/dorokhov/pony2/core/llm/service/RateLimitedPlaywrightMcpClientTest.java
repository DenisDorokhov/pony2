package net.dorokhov.pony2.core.llm.service;

import net.dorokhov.pony2.core.llm.service.RateLimitedRequestExecutor.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RateLimitedPlaywrightMcpClientTest {

    private final MutableClock clock = new MutableClock();
    private final List<Duration> sleepDurations = new ArrayList<>();

    @Mock
    private PlaywrightMcpClient playwrightMcpClient;

    @Test
    public void shouldRateLimitPlaywrightToolCalls() {

        RateLimitedPlaywrightMcpClient client = client(
                new Settings(Duration.ofSeconds(30), Duration.ZERO, 0)
        );
        when(playwrightMcpClient.call("browser_snapshot", "{}")).thenReturn("firstResult", "secondResult");

        String firstResult = client.call("browser_snapshot", "{}");
        String secondResult = client.call("browser_snapshot", "{}");

        assertThat(firstResult).isEqualTo("firstResult");
        assertThat(secondResult).isEqualTo("secondResult");
        assertThat(sleepDurations).containsExactly(Duration.ofSeconds(30));
        verify(playwrightMcpClient, times(2)).call("browser_snapshot", "{}");
    }

    private RateLimitedPlaywrightMcpClient client(Settings settings) {
        return new RateLimitedPlaywrightMcpClient(
                playwrightMcpClient,
                new RateLimitedRequestExecutor(
                        "test-playwright-mcp",
                        settings,
                        clock,
                        duration -> {
                            sleepDurations.add(duration);
                            clock.plus(duration);
                        },
                        bound -> 0
                )
        );
    }

    private static class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-09-30T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void plus(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
