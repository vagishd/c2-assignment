package com.support.ticketai;

import com.support.ticketai.config.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
public class TicketAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketAiApplication.class, args);
    }
}
