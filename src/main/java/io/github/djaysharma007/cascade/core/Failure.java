package io.github.djaysharma007.cascade.core;

import java.util.Objects;
import java.util.Optional;

/**
 * Sealed hierarchy of failures: Expected, Defect, Transient, UnknownOutcome.
 */
public sealed interface Failure {

    /**
     * Returns a human-readable explanation of why this failure occurred.
     */
    String message();

    /**
     * Returns the underlying exception cause, if available.
     */
    default Optional<Throwable> findCause() {
        return Optional.empty();
    }

    /**
     * An expected operational outcome, e.g. move-file skipped when file already exists.
     */
    record Expected(String reason) implements Failure {
        public Expected {
            Objects.requireNonNull(reason, "reason must not be null");
        }

        @Override
        public String message() {
            return reason;
        }
    }

    /**
     * A programmer error or contract violation.
     */
    record Defect(Throwable cause, String message) implements Failure {
        public Defect {
            Objects.requireNonNull(message, "message must not be null");
        }

        @Override
        public Optional<Throwable> findCause() {
            return Optional.ofNullable(cause);
        }
    }

    /**
     * Transient infrastructure error that may succeed on retry.
     */
    record Transient(String reason, Throwable cause) implements Failure {
        public Transient {
            Objects.requireNonNull(reason, "reason must not be null");
        }

        @Override
        public String message() {
            return reason;
        }

        @Override
        public Optional<Throwable> findCause() {
            return Optional.ofNullable(cause);
        }
    }

    /**
     * Unknown outcome where the caller cannot know whether the effect happened (e.g. timeout).
     */
    record UnknownOutcome(String reason, Throwable cause) implements Failure {
        public UnknownOutcome {
            Objects.requireNonNull(reason, "reason must not be null");
        }

        @Override
        public String message() {
            return reason;
        }

        @Override
        public Optional<Throwable> findCause() {
            return Optional.ofNullable(cause);
        }
    }
}
