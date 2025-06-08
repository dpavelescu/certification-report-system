package com.certreport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CertificationReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertificationReportApplication.class, args);
    }
}
