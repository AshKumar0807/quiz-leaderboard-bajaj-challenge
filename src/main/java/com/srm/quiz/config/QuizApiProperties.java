package com.srm.quiz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "quiz.api")
public class QuizApiProperties {

    @NotBlank(message = "Base URL must not be blank")
    private String baseUrl;

    @NotBlank(message = "Registration number must not be blank")
    private String regNo;

    @Positive
    private int totalPolls = 10;

    @Positive
    private long pollDelayMs = 5000;

    @Positive
    private int connectTimeoutMs = 10000;

    @Positive
    private int readTimeoutMs = 15000;
}
