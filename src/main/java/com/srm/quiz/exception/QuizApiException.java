package com.srm.quiz.exception;

/**
 * Thrown when the external quiz API returns an unexpected response
 * or a network-level error occurs that cannot be retried.
 */
public class QuizApiException extends RuntimeException {

    public QuizApiException(String message) {
        super(message);
    }

    public QuizApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
