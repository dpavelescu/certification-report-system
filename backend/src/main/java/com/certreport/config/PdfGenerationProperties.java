package com.certreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for PDF report generation
 */
@Component
@ConfigurationProperties(prefix = "report.pdf.memory-efficient")
public class PdfGenerationProperties {
    
    /**
     * Enable memory-efficient PDF generation mode for large reports
     */
    private boolean enabled = true;
    
    /**
     * Memory threshold in MB above which memory-efficient mode is automatically enabled
     */
    private int thresholdMb = 150;
    
    /**
     * Number of items to process per chunk in memory-efficient mode
     */
    private int chunkSize = 50;
    
    /**
     * Frequency of garbage collection suggestion during memory-efficient processing
     */
    private int gcFrequency = 5;
    
    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getThresholdMb() {
        return thresholdMb;
    }
    
    public void setThresholdMb(int thresholdMb) {
        this.thresholdMb = thresholdMb;
    }
    
    public int getChunkSize() {
        return chunkSize;
    }
    
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public int getGcFrequency() {
        return gcFrequency;
    }
    
    public void setGcFrequency(int gcFrequency) {
        this.gcFrequency = gcFrequency;
    }
}
