package io.github.djaysharma007.cascade.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Global user preferences and runtime settings for Cascade.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Settings(
        boolean keepRunningInTray,
        boolean startOnWindowsStartup,
        boolean reduceMotion,
        int actionTimeoutSeconds,
        int retryAttempts
) {
    public static Settings defaults() {
        return new Settings(true, false, false, 15, 3);
    }

    public Settings withKeepRunningInTray(boolean val) {
        return new Settings(val, startOnWindowsStartup, reduceMotion, actionTimeoutSeconds, retryAttempts);
    }

    public Settings withStartOnWindowsStartup(boolean val) {
        return new Settings(keepRunningInTray, val, reduceMotion, actionTimeoutSeconds, retryAttempts);
    }

    public Settings withReduceMotion(boolean val) {
        return new Settings(keepRunningInTray, startOnWindowsStartup, val, actionTimeoutSeconds, retryAttempts);
    }

    public Settings withActionTimeoutSeconds(int val) {
        return new Settings(keepRunningInTray, startOnWindowsStartup, reduceMotion, val, retryAttempts);
    }

    public Settings withRetryAttempts(int val) {
        return new Settings(keepRunningInTray, startOnWindowsStartup, reduceMotion, actionTimeoutSeconds, val);
    }
}
