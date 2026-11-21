package io.github.djaysharma007.cascade.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Shared contract test suite for Action implementations.
 */
public abstract class ActionContractTest {

    protected abstract Action createAction();

    @Test
    void executeRequiresNonNullEventAndContext() {
        Action action = createAction();
        ExecutionContext ctx = new ExecutionContext(WorkflowId.random(), RunId.random(), 0);
        Event event = Event.empty();

        assertThatThrownBy(() -> action.execute(null, ctx))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> action.execute(event, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void executeReturnsValidExecutionResultWithoutThrowing() {
        Action action = createAction();
        ExecutionContext ctx = new ExecutionContext(WorkflowId.random(), RunId.random(), 0);
        Event event = Event.empty();

        ExecutionResult result = action.execute(event, ctx);
        assertThat(result).isNotNull();
    }
}
