package com.srm.quiz.util;

import com.srm.quiz.model.dto.QuizEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class containing stateless helper methods for event deduplication.
 *
 * <p>Deduplication key format: {@code roundId_participant}
 * Events sharing the same key across different polls are considered duplicates
 * and must be counted only once.
 */
@Slf4j
public final class DeduplicationUtil {

    private DeduplicationUtil() {
        // Utility class — no instantiation
    }

    /**
     * Filters a list of events, removing any that have already been seen
     * (as tracked by {@code seenKeys}).
     *
     * @param events   incoming events from a single poll
     * @param seenKeys mutable set of deduplication keys already encountered
     * @return list of new, non-duplicate events from this batch
     */
    public static List<QuizEvent> filterDuplicates(List<QuizEvent> events,
                                                    Set<String> seenKeys) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        List<QuizEvent> unique = new ArrayList<>();

        for (QuizEvent event : events) {
            String key = event.deduplicationKey();

            if (seenKeys.contains(key)) {
                log.debug("Duplicate event skipped → key={}", key);
            } else {
                seenKeys.add(key);
                unique.add(event);
                log.debug("New event accepted → key={}, score={}", key, event.getScore());
            }
        }

        return unique;
    }

    /**
     * Creates a fresh, empty deduplication key set.
     */
    public static Set<String> createSeenKeySet() {
        return new HashSet<>();
    }
}
