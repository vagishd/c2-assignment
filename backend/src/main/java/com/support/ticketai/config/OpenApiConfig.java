package com.support.ticketai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketAiOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AI-Powered Support Ticket Management API")
                .version("v1")
                .description("Ticket CRUD, state machine, and a grounded RAG assistant over ticket history."));
    }
}
