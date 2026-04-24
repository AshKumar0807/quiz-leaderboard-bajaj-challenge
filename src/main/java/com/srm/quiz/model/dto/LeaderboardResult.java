package com.srm.quiz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Aggregated leaderboard result computed after polling and deduplication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResult {

    private List<LeaderboardEntry> leaderboard;
    private int totalScore;
    private int totalPolls;
    private int totalEventsReceived;
    private int totalDuplicatesRemoved;
    private int uniqueEventsProcessed;
    private String status;
    private String submitMessage;
    private boolean submitted;
}
