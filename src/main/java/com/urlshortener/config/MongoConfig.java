package com.urlshortener.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.urlshortener.repository")
public class MongoConfig {
    // MongoDB auto-configuration is handled by Spring Boot
    // No additional beans needed - Spring Data MongoDB handles serialization/deserialization
}

