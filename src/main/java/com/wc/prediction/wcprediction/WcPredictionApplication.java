package com.wc.prediction.wcprediction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WcPredictionApplication {

    public static void main(String[] args) {
        SpringApplication.run(WcPredictionApplication.class, args);
    }
}
