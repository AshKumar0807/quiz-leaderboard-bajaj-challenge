package com.srm.quiz.controller;

import com.srm.quiz.model.dto.ApiResponse;
import com.srm.quiz.model.dto.LeaderboardResult;
import com.srm.quiz.model.dto.SubmitResponse;
import com.srm.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing all quiz leaderboard operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/start      – triggers full polling + processing pipeline</li>
 *   <li>GET  /api/leaderboard – returns the computed leaderboard</li>
 *   <li>POST /api/submit      – submits leaderboard to external API (once)</li>
 *   <li>POST /api/reset       – resets service state for a fresh run</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * Triggers the full pipeline: poll 10 times, deduplicate, aggregate.
     * Long-running (≥45 seconds due to mandatory poll delays).
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<LeaderboardResult>> start() throws InterruptedException {
        log.info("POST /api/start — beginning quiz pipeline");
        LeaderboardResult result = quizService.processLeaderboard();
        return ResponseEntity.ok(
                ApiResponse.ok("Leaderboard computed successfully. Ready to submit.", result));
    }

    /**
     * Returns the most recently computed leaderboard without re-polling.
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<LeaderboardResult>> leaderboard() {
        log.info("GET /api/leaderboard");
        LeaderboardResult result = quizService.getLeaderboard();
        return ResponseEntity.ok(ApiResponse.ok("Leaderboard retrieved.", result));
    }

    /**
     * Submits the leaderboard to the external quiz API exactly once.
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<SubmitResponse>> submit() {
        log.info("POST /api/submit");
        SubmitResponse submitResponse = quizService.submit();
        String message = submitResponse.isCorrect()
                ? "✅ Submission accepted! " + submitResponse.getMessage()
                : "❌ Submission rejected. " + submitResponse.getMessage();
        return ResponseEntity.ok(ApiResponse.ok(message, submitResponse));
    }

    /**
     * Resets the service so the pipeline can be re-run.
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> reset() {
        log.info("POST /api/reset");
        quizService.reset();
        return ResponseEntity.ok(ApiResponse.ok("Service reset. You can now call /api/start again.", null));
    }
}
