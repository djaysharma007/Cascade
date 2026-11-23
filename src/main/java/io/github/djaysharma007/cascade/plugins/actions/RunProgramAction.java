package io.github.djaysharma007.cascade.plugins.actions;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.Failure;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.registry.TemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Runs an external executable program with argument substitution and timeout enforcement.
 */
@Plugin(id = "run-program", displayName = "Run program", category = PluginCategory.ACTION)
public final class RunProgramAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(RunProgramAction.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final String command;
    private final String arguments;
    private final String workingDirectory;
    private final int timeoutSeconds;

    public RunProgramAction(String command, String arguments, String workingDirectory, Integer timeoutSeconds) {
        Objects.requireNonNull(command, "command must not be null");
        this.command = command.trim();
        this.arguments = arguments != null ? arguments.trim() : "";
        this.workingDirectory = workingDirectory != null ? workingDirectory.trim() : "";
        this.timeoutSeconds = timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }

    @Override
    public ExecutionResult execute(Event event, ExecutionContext context) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(context, "context must not be null");

        String resolvedCmd = TemplateResolver.resolve(command, event, context, null);
        String resolvedArgs = TemplateResolver.resolve(arguments, event, context, null);

        List<String> commandTokens = new ArrayList<>();
        commandTokens.add(resolvedCmd);
        if (!resolvedArgs.isBlank()) {
            for (String arg : resolvedArgs.split("\\s+")) {
                if (!arg.isBlank()) {
                    commandTokens.add(arg);
                }
            }
        }

        ProcessBuilder pb = new ProcessBuilder(commandTokens);
        pb.redirectErrorStream(true);

        if (!workingDirectory.isBlank()) {
            File workDir = new File(TemplateResolver.resolve(workingDirectory, event, context, null));
            if (workDir.isDirectory()) {
                pb.directory(workDir);
            }
        }

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (output.length() < 10_000) {
                            output.append(line).append("\n");
                        }
                    }
                } catch (Exception ignored) {}
            });
            readerThread.setDaemon(true);
            readerThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ExecutionResult.Failed(
                        new Failure.Transient("Program timed out after " + timeoutSeconds + " seconds: " + resolvedCmd)
                );
            }

            int exitCode = process.exitValue();
            String stdOut = output.toString().trim();

            if (exitCode != 0) {
                log.warn("Program exited with code {}: {}", exitCode, stdOut);
                return new ExecutionResult.Failed(
                        new Failure.Expected("Program exited with non-zero code " + exitCode + ": " + stdOut)
                );
            }

            log.info("Program completed successfully: {}", resolvedCmd);
            return new ExecutionResult.Success(Map.of(
                    "exitCode", exitCode,
                    "output", stdOut
            ));
        } catch (Exception e) {
            log.error("Failed to execute program {}", resolvedCmd, e);
            return new ExecutionResult.Failed(new Failure.Defect(e, "Error executing program: " + e.getMessage()));
        }
    }

    public String getCommand() {
        return command;
    }

    public String getArguments() {
        return arguments;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
