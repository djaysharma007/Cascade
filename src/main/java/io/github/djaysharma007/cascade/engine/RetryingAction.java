package io.github.djaysharma007.cascade.engine;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Decorator over an Action that retries transient failures with exponential backoff and jitter.
 * Defaults follow PRD section 8: 3 attempts, 500 ms first delay doubling, full jitter, capped at 10 s, 30 s total budget.
 */
public final class RetryingAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(RetryingAction.class);

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_BASE_DELAY_MS = 500L;
    public static final long DEFAULT_MAX_DELAY_MS = 10_000L;
    public static final long DEFAULT_TOTAL_BUDGET_MS = 30_000L;

    private final Action delegate;
    private final int maxAttempts;
    private final long baseDelayMs;
    private final long maxDelayMs;
    private final long totalBudgetMs;
    private int attemptsExecuted;

    public RetryingAction(Action delegate) {
        this(delegate, DEFAULT_MAX_ATTEMPTS, DEFAULT_BASE_DELAY_MS, DEFAULT_MAX_DELAY_MS, DEFAULT_TOTAL_BUDGET_MS);
    }

    public RetryingAction(Action delegate, int maxAttempts, long baseDelayMs, long maxDelayMs, long totalBudgetMs) {
        this.delegate = Objects.requireNonNull(delegate, "delegate action must not be null");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1, got " + maxAttempts);
        }
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.totalBudgetMs = totalBudgetMs;
    }

    @Override
    public ExecutionResult execute(Event event, ExecutionContext context) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(context, "context must not be null");

        long startEpochMs = System.currentTimeMillis();
        ExecutionResult lastResult = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            this.attemptsExecuted = attempt;
            long elapsed = System.currentTimeMillis() - startEpochMs;
            if (attempt > 1 && elapsed >= totalBudgetMs) {
                log.warn("Retry budget of {} ms exceeded (elapsed: {} ms). Aborting retries.", totalBudgetMs, elapsed);
                break;
            }

            lastResult = delegate.execute(event, context);

            if (lastResult instanceof ExecutionResult.Success || lastResult instanceof ExecutionResult.Skipped) {
                return lastResult;
            }

            if (lastResult instanceof ExecutionResult.Failed failed) {
                if (failed.failure() instanceof Failure.Transient transientFailure) {
                    if (attempt < maxAttempts && (System.currentTimeMillis() - startEpochMs) < totalBudgetMs) {
                        long maxBackoffForAttempt = Math.min(maxDelayMs, baseDelayMs * (1L << (attempt - 1)));
                        long backoffMs = ThreadLocalRandom.current().nextLong(baseDelayMs / 2, maxBackoffForAttempt + 1);

                        log.warn("Action attempt {}/{} failed with transient error: {}. Retrying in {} ms...",
                                attempt, maxAttempts, transientFailure.message(), backoffMs);

                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("Action retry interrupted on attempt {}", attempt, e);
                            return new ExecutionResult.Failed(
                                    new Failure.Transient("Action retry interrupted: " + e.getMessage(), e)
                            );
                        }
                    } else {
                        log.error("Action exhausted all {} retry attempts. Final transient failure: {}",
                                maxAttempts, transientFailure.message(), transientFailure.findCause().orElse(null));
                        return lastResult;
                    }
                } else {
                    // Non-transient failure (Expected or Defect): do not retry
                    return lastResult;
                }
            }
        }

        return lastResult != null ? lastResult : new ExecutionResult.Failed(
                new Failure.Defect(null, "Action failed with no result produced")
        );
    }

    public int attemptsExecuted() {
        return attemptsExecuted;
    }

    public Action delegate() {
        return delegate;
    }
}
