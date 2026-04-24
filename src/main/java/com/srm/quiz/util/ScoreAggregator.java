package com.srm.quiz.util;

import com.srm.quiz.model.dto.LeaderboardEntry;
import com.srm.quiz.model.dto.QuizEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Stateless utility for aggregating quiz event scores into a leaderboard.
 */
@Slf4j
public final class ScoreAggregator {

    private ScoreAggregator() {
        // Utility class — no instantiation
    }

    /**
     * Aggregates scores from a list of unique events into a sorted leaderboard.
     *
     * <p>Steps:
     * <ol>
     *   <li>Accumulate scores per participant using a {@link Map}</li>
     *   <li>Sort descending by total score</li>
     *   <li>Assign ranks (ties share the same rank)</li>
     * </ol>
     *
     * @param uniqueEvents deduplicated events
     * @return sorted leaderboard entries
     */
    public static List<LeaderboardEntry> aggregate(List<QuizEvent> uniqueEvents) {
        if (uniqueEvents == null || uniqueEvents.isEmpty()) {
            log.warn("No events to aggregate — returning empty leaderboard");
            return List.of();
        }

        // Step 1: Accumulate scores per participant
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        for (QuizEvent event : uniqueEvents) {
            scoreMap.merge(event.getParticipant(), event.getScore(), Integer::sum);
        }

        log.debug("Score map after aggregation: {}", scoreMap);

        // Step 2: Sort descending by total score
        List<Map.Entry<String, Integer>> sorted = scoreMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))   // stable secondary sort
                .collect(Collectors.toList());

        // Step 3: Assign ranks (ties share the same rank)
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        AtomicInteger rank = new AtomicInteger(1);
        int previousScore = -1;
        int currentRank = 1;

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            int score = entry.getValue();

            if (i > 0 && score != previousScore) {
                currentRank = rank.get();
            }

            leaderboard.add(LeaderboardEntry.builder()
                    .participant(entry.getKey())
                    .totalScore(score)
                    .rank(currentRank)
                    .build());

            previousScore = score;
            rank.incrementAndGet();
        }

        return leaderboard;
    }

    /**
     * Computes the grand total across all participants.
     *
     * @param leaderboard sorted leaderboard entries
     * @return sum of all total scores
     */
    public static int computeTotal(List<LeaderboardEntry> leaderboard) {
        return leaderboard.stream()
                .mapToInt(LeaderboardEntry::getTotalScore)
                .sum();
    }
}
