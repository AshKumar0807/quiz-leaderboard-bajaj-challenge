package com.srm.quiz.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single quiz event returned in poll responses.
 * Each event belongs to a round and carries a participant's score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizEvent {

    private String roundId;
    private String participant;
    private int score;

    /**
     * Unique deduplication key composed of roundId and participant.
     * Two events with the same key are considered duplicates.
     */
    public String deduplicationKey() {
        return roundId + "_" + participant;
    }
}
