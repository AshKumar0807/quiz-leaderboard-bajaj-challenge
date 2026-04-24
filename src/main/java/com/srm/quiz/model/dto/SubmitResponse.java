package com.srm.quiz.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body received from the quiz submit endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitResponse {

    private boolean isCorrect;
    private boolean isIdempotent;
    private int submittedTotal;
    private int expectedTotal;
    private String message;
}
