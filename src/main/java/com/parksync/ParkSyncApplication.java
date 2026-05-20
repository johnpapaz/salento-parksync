package com.parksync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ParkSyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParkSyncApplication.class, args);
    }
}
