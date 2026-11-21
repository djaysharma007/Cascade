package io.github.djaysharma007.cascade.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaboteurContractRejectTest {

    // Deliberately broken Saboteur Action that throws RuntimeException instead of returning a Failure
    static class SaboteurAction implements Action {
        @Override
        public ExecutionResult execute(Event event, ExecutionContext context) {
            throw new RuntimeException("Saboteur breached contract!");
        }
    }

    // Deliberately broken Saboteur Trigger that allows multiple starts and throws on stop
    static class SaboteurTrigger implements Trigger {
        @Override
        public void start(TriggerListener listener) {
            // Violates: does not throw IllegalStateException when started again
        }

        @Override
        public void stop() {
            throw new RuntimeException("Saboteur throw on stop!");
        }
    }

    @Test
    void contractSuiteRejectsSaboteurAction() {
        Action action = new SaboteurAction();
        ExecutionContext ctx = new ExecutionContext(WorkflowId.random(), RunId.random(), 0);
        Event event = Event.empty();

        // Must fail the contract expectation of returning a result without throwing
        assertThatThrownBy(() -> action.execute(event, ctx))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Saboteur breached contract");
    }

    @Test
    void contractSuiteRejectsSaboteurTrigger() {
        Trigger trigger = new SaboteurTrigger();
        // Stop contract says stop is idempotent and never throws
        assertThatThrownBy(trigger::stop)
                .isInstanceOf(RuntimeException.class);
    }
}
