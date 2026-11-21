package io.github.djaysharma007.cascade.core;

import java.util.Objects;

/**
 * Sealed hierarchy of failures: Expected, Defect, Transient, UnknownOutcome.
 */
public sealed interface Failure {

    /**
     * An expected operational outcome, e.g. move-file skipped when file already exists.
     */
    record Expected(String reason) implements Failure {
        public Expected {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * A programmer error or contract violation.
     */
    record Defect(Throwable cause, String message) implements Failure {
        public Defect {
            Objects.requireNonNull(message, "message must not be null");
        }
    }

    /**
     * Transient infrastructure error that may succeed on retry.
     */
    record Transient(String reason, Throwable cause) implements Failure {
        public Transient {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * Unknown outcome where the caller cannot know whether the effect happened (e.g. timeout).
     */
    record UnknownOutcome(String reason, Throwable cause) implements Failure {
        public UnknownOutcome {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
