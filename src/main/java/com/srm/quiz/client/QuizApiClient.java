package com.srm.quiz.client;

import com.srm.quiz.config.QuizApiProperties;
import com.srm.quiz.exception.QuizApiException;
import com.srm.quiz.model.dto.PollResponse;
import com.srm.quiz.model.dto.SubmitRequest;
import com.srm.quiz.model.dto.SubmitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * HTTP client for communicating with the external Quiz API.
 *
 * Poll endpoint : GET  /quiz/messages?regNo={regNo}&poll={0..9}
 * Submit endpoint: POST /quiz/submit
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizApiClient {

    private final RestTemplate restTemplate;
    private final QuizApiProperties properties;

    /**
     * Fetches one poll from the external API.
     *
     * @param pollIndex value 0–9
     * @return parsed PollResponse
     */
    public PollResponse fetchPoll(int pollIndex) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.getBaseUrl() + "/quiz/messages")
                .queryParam("regNo", properties.getRegNo())
                .queryParam("poll", pollIndex)
                .build()
                .toUri();

        log.info("Fetching poll {} → {}", pollIndex, uri);

        try {
            PollResponse response = restTemplate.getForObject(uri, PollResponse.class);
            if (response == null) {
                throw new QuizApiException("Null response received for poll " + pollIndex);
            }
            return response;
        } catch (QuizApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QuizApiException("Failed to fetch poll " + pollIndex + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Submits the computed leaderboard to the external API.
     *
     * @param request leaderboard payload
     * @return submit response
     */
    public SubmitResponse submitLeaderboard(SubmitRequest request) {
        String url = properties.getBaseUrl() + "/quiz/submit";
        log.info("Submitting leaderboard to {}", url);

        try {
            SubmitResponse response = restTemplate.postForObject(url, request, SubmitResponse.class);
            if (response == null) {
                throw new QuizApiException("Null response received from submit endpoint");
            }
            return response;
        } catch (QuizApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QuizApiException("Failed to submit leaderboard: " + ex.getMessage(), ex);
        }
    }
}
