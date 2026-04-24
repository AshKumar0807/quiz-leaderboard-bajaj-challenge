package com.srm.quiz.service;

import com.srm.quiz.client.QuizApiClient;
import com.srm.quiz.config.QuizApiProperties;
import com.srm.quiz.model.dto.*;
import com.srm.quiz.util.DeduplicationUtil;
import com.srm.quiz.util.ScoreAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Core business logic for the Quiz Leaderboard System.
 *
 * <p>Orchestrates:
 * <ol>
 *   <li>Polling the external API 10 times with mandatory 5-second delay</li>
 *   <li>Deduplicating events across polls</li>
 *   <li>Aggregating scores per participant</li>
 *   <li>Building the sorted leaderboard</li>
 *   <li>Submitting exactly once</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizApiClient quizApiClient;
    private final QuizApiProperties properties;

    /** Guards against double submission. */
    private final AtomicBoolean submitted = new AtomicBoolean(false);

    /** Cached result after processing. */
    private volatile LeaderboardResult cachedResult;

    // ─── Public API ─────────────────────────────────────────────────────────────

    /**
     * Runs the full pipeline: poll → deduplicate → aggregate → return result.
     * Does NOT automatically submit; call {@link #submit()} separately.
     *
     * @return computed leaderboard result
     * @throws InterruptedException if the delay sleep is interrupted
     */
    public LeaderboardResult processLeaderboard() throws InterruptedException {
        log.info("═══════════════════════════════════════════════════");
        log.info("Starting Quiz Leaderboard Pipeline");
        log.info("regNo={}, totalPolls={}, delayMs={}",
                properties.getRegNo(), properties.getTotalPolls(), properties.getPollDelayMs());
        log.info("═══════════════════════════════════════════════════");

        // Reset submission guard when re-processing
        submitted.set(false);
        cachedResult = null;

        Set<String> seenKeys = DeduplicationUtil.createSeenKeySet();
        List<QuizEvent> allUniqueEvents = new ArrayList<>();
        int totalEventsReceived = 0;

        for (int pollIndex = 0; pollIndex < properties.getTotalPolls(); pollIndex++) {
            log.info("─── Poll {}/{} ───────────────────────────────────",
                    pollIndex + 1, properties.getTotalPolls());

            PollResponse pollResponse = quizApiClient.fetchPoll(pollIndex);

            List<QuizEvent> rawEvents =
                    pollResponse.getEvents() == null ? List.of() : pollResponse.getEvents();
            totalEventsReceived += rawEvents.size();

            log.info("Poll {} received {} raw event(s)", pollIndex, rawEvents.size());

            List<QuizEvent> newUniqueEvents =
                    DeduplicationUtil.filterDuplicates(rawEvents, seenKeys);
            allUniqueEvents.addAll(newUniqueEvents);

            log.info("Poll {} added {} new unique event(s) ({} duplicate(s) discarded)",
                    pollIndex,
                    newUniqueEvents.size(),
                    rawEvents.size() - newUniqueEvents.size());

            // Mandatory 5-second delay between polls (skip after last poll)
            if (pollIndex < properties.getTotalPolls() - 1) {
                log.info("Waiting {}ms before next poll...", properties.getPollDelayMs());
                Thread.sleep(properties.getPollDelayMs());
            }
        }

        int duplicatesRemoved = totalEventsReceived - allUniqueEvents.size();

        log.info("═══════════════════════════════════════════════════");
        log.info("Polling complete. Total received={}, unique={}, duplicates={}",
                totalEventsReceived, allUniqueEvents.size(), duplicatesRemoved);

        // Aggregate and build leaderboard
        List<LeaderboardEntry> leaderboard = ScoreAggregator.aggregate(allUniqueEvents);
        int totalScore = ScoreAggregator.computeTotal(leaderboard);

        log.info("Leaderboard computed. Participants={}, totalScore={}",
                leaderboard.size(), totalScore);
        leaderboard.forEach(e -> log.info("  Rank {} │ {} │ score={}",
                e.getRank(), e.getParticipant(), e.getTotalScore()));
        log.info("═══════════════════════════════════════════════════");

        cachedResult = LeaderboardResult.builder()
                .leaderboard(leaderboard)
                .totalScore(totalScore)
                .totalPolls(properties.getTotalPolls())
                .totalEventsReceived(totalEventsReceived)
                .totalDuplicatesRemoved(duplicatesRemoved)
                .uniqueEventsProcessed(allUniqueEvents.size())
                .status("READY")
                .submitted(false)
                .build();

        return cachedResult;
    }

    /**
     * Returns the previously computed leaderboard without re-polling.
     *
     * @return cached leaderboard result
     * @throws IllegalStateException if {@link #processLeaderboard()} hasn't been called yet
     */
    public LeaderboardResult getLeaderboard() {
        if (cachedResult == null) {
            throw new IllegalStateException(
                    "Leaderboard not yet computed. Call /api/start first.");
        }
        return cachedResult;
    }

    /**
     * Submits the leaderboard to the external API exactly once.
     *
     * @return the submit response from the external API
     * @throws IllegalStateException if called before processing or if already submitted
     */
    public SubmitResponse submit() {
        if (cachedResult == null) {
            throw new IllegalStateException(
                    "Cannot submit — leaderboard not computed. Call /api/start first.");
        }

        if (!submitted.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "Leaderboard has already been submitted. Duplicate submissions are not allowed.");
        }

        log.info("Preparing leaderboard submission...");

        List<SubmitRequest.LeaderboardEntrySubmit> submitEntries =
                cachedResult.getLeaderboard().stream()
                        .map(e -> SubmitRequest.LeaderboardEntrySubmit.builder()
                                .participant(e.getParticipant())
                                .totalScore(e.getTotalScore())
                                .build())
                        .collect(Collectors.toList());

        SubmitRequest request = SubmitRequest.builder()
                .regNo(properties.getRegNo())
                .leaderboard(submitEntries)
                .build();

        SubmitResponse response = quizApiClient.submitLeaderboard(request);

        // Update cached result with submission info
        cachedResult.setSubmitted(true);
        cachedResult.setStatus(response.isCorrect() ? "SUBMITTED_CORRECT" : "SUBMITTED_INCORRECT");
        cachedResult.setSubmitMessage(response.getMessage());

        log.info("Submission result: correct={}, message={}", response.isCorrect(), response.getMessage());

        return response;
    }

    /**
     * Resets the service state, allowing a fresh run.
     */
    public void reset() {
        submitted.set(false);
        cachedResult = null;
        log.info("QuizService state has been reset.");
    }
}
