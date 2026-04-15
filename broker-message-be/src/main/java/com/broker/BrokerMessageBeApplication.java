package com.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrokerMessageBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerMessageBeApplication.class, args);
    }
}
