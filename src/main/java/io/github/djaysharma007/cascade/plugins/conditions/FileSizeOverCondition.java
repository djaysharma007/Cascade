package io.github.djaysharma007.cascade.plugins.conditions;

import io.github.djaysharma007.cascade.core.Condition;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;

import java.util.Objects;

/**
 * Checks whether the event file size in megabytes exceeds the specified threshold.
 */
@Plugin(id = "file-size-over", displayName = "File size over", category = PluginCategory.CONDITION)
public final class FileSizeOverCondition implements Condition {

    private static final long BYTES_PER_MB = 1024L * 1024L;
    private final double thresholdMb;

    public FileSizeOverCondition(double thresholdMb) {
        if (thresholdMb < 0) {
            throw new IllegalArgumentException("thresholdMb must not be negative");
        }
        this.thresholdMb = thresholdMb;
    }

    @Override
    public boolean test(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        Object sizeObj = event.payload().get("fileSize");
        if (sizeObj instanceof Number num) {
            double mb = num.doubleValue() / BYTES_PER_MB;
            return mb > thresholdMb;
        }
        return false;
    }
}
