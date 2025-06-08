package com.certreport.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration class for application metrics and monitoring
 */
@Configuration
public class MetricsConfig {

    private final AtomicInteger activeReportGenerations = new AtomicInteger(0);
    private final AtomicInteger totalReportsGenerated = new AtomicInteger(0);
    private final AtomicInteger failedReportsCount = new AtomicInteger(0);

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
    public Gauge activeReportGenerationsGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("reports.active.count", this, MetricsConfig::getActiveReportGenerations)
                .description("Number of currently active report generations")
                .register(meterRegistry);
    }

    @Bean
    public Gauge memoryUsageGauge(MeterRegistry meterRegistry) {
        return Gauge.builder("jvm.memory.used.bytes", this, config -> {
                    Runtime runtime = Runtime.getRuntime();
                    return (double) (runtime.totalMemory() - runtime.freeMemory());
                })
                .description("JVM memory usage during report generation")
                .register(meterRegistry);
    }

    // Utility methods for metric tracking
    public void incrementActiveReportGenerations() {
        activeReportGenerations.incrementAndGet();
    }

    public void decrementActiveReportGenerations() {
        activeReportGenerations.decrementAndGet();
    }

    public int getActiveReportGenerations() {
        return activeReportGenerations.get();
    }

    public void incrementTotalReportsGenerated() {
        totalReportsGenerated.incrementAndGet();
    }

    public void incrementFailedReportsCount() {
        failedReportsCount.incrementAndGet();
    }

    public int getTotalReportsGenerated() {
        return totalReportsGenerated.get();
    }

    public int getFailedReportsCount() {
        return failedReportsCount.get();
    }
}
