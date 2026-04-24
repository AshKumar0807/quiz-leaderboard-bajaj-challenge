package com.srm.quiz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Request body sent to the quiz submit endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRequest {

    private String regNo;
    private List<LeaderboardEntrySubmit> leaderboard;

    /**
     * Lightweight DTO used specifically for serialization in the submit payload.
     * Does NOT include the rank field (not required by the API).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntrySubmit {
        private String participant;
        private int totalScore;
    }
}
