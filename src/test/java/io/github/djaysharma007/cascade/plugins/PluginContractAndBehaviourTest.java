package io.github.djaysharma007.cascade.plugins;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.ActionContractTest;
import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.ConditionContractTest;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.RunId;
import io.github.djaysharma007.cascade.core.Trigger;
import io.github.djaysharma007.cascade.core.TriggerContractTest;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.plugins.actions.MoveFileAction;
import io.github.djaysharma007.cascade.plugins.actions.PrintMessageAction;
import io.github.djaysharma007.cascade.plugins.conditions.AllOfCondition;
import io.github.djaysharma007.cascade.plugins.conditions.AnyOfCondition;
import io.github.djaysharma007.cascade.plugins.conditions.ExtensionIsCondition;
import io.github.djaysharma007.cascade.plugins.conditions.FileSizeOverCondition;
import io.github.djaysharma007.cascade.plugins.conditions.NameContainsCondition;
import io.github.djaysharma007.cascade.plugins.conditions.NotCondition;
import io.github.djaysharma007.cascade.plugins.triggers.ManualTrigger;
import io.github.djaysharma007.cascade.plugins.triggers.TimerTrigger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PluginContractAndBehaviourTest {

    @Nested
    class ManualTriggerContractTest extends TriggerContractTest {
        @Override
        protected Trigger createTrigger() {
            return new ManualTrigger("Test Manual");
        }
    }

    @Nested
    class TimerTriggerContractTest extends TriggerContractTest {
        @Override
        protected Trigger createTrigger() {
            return new TimerTrigger(60, false);
        }
    }

    @Nested
    class ExtensionIsContractTest extends ConditionContractTest {
        @Override
        protected Condition createCondition() {
            return new ExtensionIsCondition(".pdf");
        }

        @Test
        void evaluatesExtension() {
            Condition c = createCondition();
            assertThat(c.test(Event.of(Map.of("fileName", "doc.PDF")))).isTrue();
            assertThat(c.test(Event.of(Map.of("fileName", "doc.txt")))).isFalse();
            assertThat(c.test(Event.empty())).isFalse();
        }
    }

    @Nested
    class NameContainsContractTest extends ConditionContractTest {
        @Override
        protected Condition createCondition() {
            return new NameContainsCondition("assignment", false);
        }

        @Test
        void evaluatesSubstring() {
            Condition c = createCondition();
            assertThat(c.test(Event.of(Map.of("fileName", "My_Assignment_1.pdf")))).isTrue();
            assertThat(c.test(Event.of(Map.of("fileName", "photo.png")))).isFalse();
        }
    }

    @Nested
    class FileSizeOverContractTest extends ConditionContractTest {
        @Override
        protected Condition createCondition() {
            return new FileSizeOverCondition(1.0);
        }

        @Test
        void evaluatesSize() {
            Condition c = createCondition();
            // 2 MB
            assertThat(c.test(Event.of(Map.of("fileSize", 2L * 1024 * 1024)))).isTrue();
            // 0.5 MB
            assertThat(c.test(Event.of(Map.of("fileSize", 512L * 1024)))).isFalse();
        }
    }

    @Nested
    class CompositesTest {
        @Test
        void allOfAndAnyOfAndNot() {
            Condition isPdf = new ExtensionIsCondition(".pdf");
            Condition isDoc = new ExtensionIsCondition(".docx");

            Condition allOf = new AllOfCondition(List.of(isPdf));
            assertThat(allOf.test(Event.of(Map.of("fileName", "test.pdf")))).isTrue();

            Condition anyOf = new AnyOfCondition(List.of(isPdf, isDoc));
            assertThat(anyOf.test(Event.of(Map.of("fileName", "test.docx")))).isTrue();
            assertThat(anyOf.test(Event.of(Map.of("fileName", "test.png")))).isFalse();

            Condition not = new NotCondition(isPdf);
            assertThat(not.test(Event.of(Map.of("fileName", "test.png")))).isTrue();
            assertThat(not.test(Event.of(Map.of("fileName", "test.pdf")))).isFalse();
        }
    }

    @Nested
    class PrintMessageContractTest extends ActionContractTest {
        @Override
        protected Action createAction() {
            return new PrintMessageAction("Hello {fileName}");
        }

        @Test
        void executesSuccessfully() {
            Action action = createAction();
            ExecutionContext ctx = new ExecutionContext(WorkflowId.random(), RunId.random(), 0);
            Event event = Event.of(Map.of("fileName", "demo.pdf"));
            ExecutionResult res = action.execute(event, ctx);
            assertThat(res).isInstanceOf(ExecutionResult.Success.class);
        }
    }

    @Nested
    class MoveFileActionTest extends ActionContractTest {
        @TempDir
        Path tempDir;

        @Override
        protected Action createAction() {
            return new MoveFileAction(tempDir.toString(), "rename");
        }

        @Test
        void movesFileWithRenamePolicy() throws IOException {
            Path srcFile = tempDir.resolve("sample.txt");
            Files.writeString(srcFile, "Hello");

            Path destDir = tempDir.resolve("dest");
            Action action = new MoveFileAction(destDir.toString(), "rename");
            ExecutionContext ctx = new ExecutionContext(WorkflowId.random(), RunId.random(), 0);
            Event event = Event.of(Map.of("filePath", srcFile.toAbsolutePath().toString()));

            ExecutionResult res = action.execute(event, ctx);
            assertThat(res).isInstanceOf(ExecutionResult.Success.class);
            assertThat(destDir.resolve("sample.txt")).exists();
            assertThat(srcFile).doesNotExist();
        }
    }
}
