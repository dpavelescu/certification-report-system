package com.certreport.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Simple metrics configuration - let Micrometer handle complexity
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter reportGenerationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("reports.generated.total")
                .description("Total number of reports generated")
                .register(meterRegistry);
    }

    @Bean
    public Counter reportFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("reports.failed.total")
                .description("Total number of failed report generations")
                .register(meterRegistry);
    }

    @Bean
    public Timer reportGenerationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("reports.generation.duration")
                .description("Time taken to generate reports")
                .register(meterRegistry);
    }    @Bean
    public Gauge memoryUsageGauge(MeterRegistry meterRegistry) {
        // Register gauge using the correct builder pattern
        return Gauge.builder("jvm.memory.used.bytes", () -> {
                    Runtime runtime = Runtime.getRuntime();
                    return (double) (runtime.totalMemory() - runtime.freeMemory());
                })
                .description("JVM memory usage during report generation")
                .register(meterRegistry);
    }
}
