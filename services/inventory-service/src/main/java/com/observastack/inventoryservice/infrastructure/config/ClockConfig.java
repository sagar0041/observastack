package com.observastack.inventoryservice.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies the {@link Clock} the domain layer uses to stamp
 * reservation timestamps, so tests can substitute a fixed clock instead
 * of depending on wall-clock time.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
