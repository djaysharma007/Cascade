package io.github.djaysharma007.cascade.engine;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.Failure;
import io.github.djaysharma007.cascade.core.RunId;
import io.github.djaysharma007.cascade.core.WorkflowId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RetryingActionTest {

    private final ExecutionContext context = new ExecutionContext(WorkflowId.random(), RunId.random(), 0);
    private final Event event = Event.empty();

    @Test
    void immediateSuccessExecutesOnce() {
        AtomicInteger count = new AtomicInteger(0);
        Action delegate = (e, ctx) -> {
            count.incrementAndGet();
            return new ExecutionResult.Success(Map.of("res", "ok"));
        };

        RetryingAction retrying = new RetryingAction(delegate, 3, 10, 50, 1000);
        ExecutionResult res = retrying.execute(event, context);

        assertThat(res).isInstanceOf(ExecutionResult.Success.class);
        assertThat(count.get()).isEqualTo(1);
        assertThat(retrying.attemptsExecuted()).isEqualTo(1);
    }

    @Test
    void transientFailureThenSuccessRetriesAndSucceeds() {
        AtomicInteger count = new AtomicInteger(0);
        Action delegate = (e, ctx) -> {
            int attempt = count.incrementAndGet();
            if (attempt == 1) {
                return new ExecutionResult.Failed(new Failure.Transient("File locked", new IOException("Locked")));
            }
            return new ExecutionResult.Success(Map.of("res", "done"));
        };

        RetryingAction retrying = new RetryingAction(delegate, 3, 10, 50, 1000);
        ExecutionResult res = retrying.execute(event, context);

        assertThat(res).isInstanceOf(ExecutionResult.Success.class);
        assertThat(count.get()).isEqualTo(2);
        assertThat(retrying.attemptsExecuted()).isEqualTo(2);
    }

    @Test
    void transientFailureExhaustsAllAttempts() {
        AtomicInteger count = new AtomicInteger(0);
        Action delegate = (e, ctx) -> {
            count.incrementAndGet();
            return new ExecutionResult.Failed(new Failure.Transient("File locked", new IOException("Locked")));
        };

        RetryingAction retrying = new RetryingAction(delegate, 3, 10, 50, 1000);
        ExecutionResult res = retrying.execute(event, context);

        assertThat(res).isInstanceOf(ExecutionResult.Failed.class);
        ExecutionResult.Failed failed = (ExecutionResult.Failed) res;
        assertThat(failed.failure()).isInstanceOf(Failure.Transient.class);
        assertThat(failed.failure().message()).isEqualTo("File locked");
        assertThat(count.get()).isEqualTo(3);
        assertThat(retrying.attemptsExecuted()).isEqualTo(3);
    }

    @Test
    void expectedFailureDoesNotRetry() {
        AtomicInteger count = new AtomicInteger(0);
        Action delegate = (e, ctx) -> {
            count.incrementAndGet();
            return new ExecutionResult.Failed(new Failure.Expected("Destination exists, skip"));
        };

        RetryingAction retrying = new RetryingAction(delegate, 3, 10, 50, 1000);
        ExecutionResult res = retrying.execute(event, context);

        assertThat(res).isInstanceOf(ExecutionResult.Failed.class);
        ExecutionResult.Failed failed = (ExecutionResult.Failed) res;
        assertThat(failed.failure()).isInstanceOf(Failure.Expected.class);
        assertThat(count.get()).isEqualTo(1);
        assertThat(retrying.attemptsExecuted()).isEqualTo(1);
    }

    @Test
    void defectFailureDoesNotRetry() {
        AtomicInteger count = new AtomicInteger(0);
        Action delegate = (e, ctx) -> {
            count.incrementAndGet();
            return new ExecutionResult.Failed(new Failure.Defect(null, "Null pointer bug"));
        };

        RetryingAction retrying = new RetryingAction(delegate, 3, 10, 50, 1000);
        ExecutionResult res = retrying.execute(event, context);

        assertThat(res).isInstanceOf(ExecutionResult.Failed.class);
        ExecutionResult.Failed failed = (ExecutionResult.Failed) res;
        assertThat(failed.failure()).isInstanceOf(Failure.Defect.class);
        assertThat(count.get()).isEqualTo(1);
        assertThat(retrying.attemptsExecuted()).isEqualTo(1);
    }
}
