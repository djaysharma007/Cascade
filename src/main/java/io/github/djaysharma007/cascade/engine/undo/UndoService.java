package io.github.djaysharma007.cascade.engine.undo;

import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.RunId;
import io.github.djaysharma007.cascade.engine.RunOutcome;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.engine.StepRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing reversible workflow operations (Move, Copy, Rename) via the Command pattern.
 */
public final class UndoService {

    private static final Logger log = LoggerFactory.getLogger(UndoService.class);
    private final Set<RunId> undoneRuns = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public boolean canUndo(RunRecord run) {
        if (run == null || run.outcome() != RunOutcome.SUCCEEDED || undoneRuns.contains(run.id())) {
            return false;
        }
        for (StepRecord step : run.steps()) {
            if (step.result() instanceof ExecutionResult.Success succ && !succ.output().isEmpty()) {
                String id = step.actionId().value();
                if ("move-file".equals(id) || "copy-file".equals(id) || "rename-file".equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isUndone(RunId runId) {
        return undoneRuns.contains(runId);
    }

    public List<UndoCommand> buildUndoCommands(RunRecord run) {
        Objects.requireNonNull(run, "run must not be null");
        List<UndoCommand> commands = new ArrayList<>();
        List<StepRecord> steps = run.steps();

        // Process in reverse chronological order for accurate reversal stack
        for (int i = steps.size() - 1; i >= 0; i--) {
            StepRecord step = steps.get(i);
            if (step.result() instanceof ExecutionResult.Success succ) {
                Map<String, Object> out = succ.output();
                String actionId = step.actionId().value();

                switch (actionId) {
                    case "move-file" -> {
                        Object movedTo = out.get("movedTo");
                        Object source = out.get("sourcePath");
                        if (movedTo != null && source != null) {
                            commands.add(new UndoMoveCommand(Path.of(source.toString()), Path.of(movedTo.toString())));
                        }
                    }
                    case "copy-file" -> {
                        Object copiedTo = out.get("copiedTo");
                        if (copiedTo != null) {
                            commands.add(new UndoCopyCommand(Path.of(copiedTo.toString())));
                        }
                    }
                    case "rename-file" -> {
                        Object renamedTo = out.get("renamedTo");
                        Object source = out.get("sourcePath");
                        if (renamedTo != null && source != null) {
                            commands.add(new UndoRenameCommand(Path.of(source.toString()), Path.of(renamedTo.toString())));
                        }
                    }
                    default -> {}
                }
            }
        }
        return commands;
    }

    public void undoRun(RunRecord run) throws IOException {
        Objects.requireNonNull(run, "run must not be null");
        if (undoneRuns.contains(run.id())) {
            throw new IllegalStateException("Run " + run.id().value() + " has already been undone");
        }

        List<UndoCommand> commands = buildUndoCommands(run);
        if (commands.isEmpty()) {
            throw new IllegalStateException("Run " + run.id().value() + " has no undoable actions");
        }

        for (UndoCommand cmd : commands) {
            cmd.execute();
            log.info("Undid action: {}", cmd.description());
        }

        undoneRuns.add(run.id());
    }
}
