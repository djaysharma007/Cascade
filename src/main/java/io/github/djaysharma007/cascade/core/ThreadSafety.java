package io.github.djaysharma007.cascade.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents and classifies the concurrency guard or confinement of fields.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface ThreadSafety {
    Category value();
    String guard() default "";

    enum Category {
        IMMUTABLE,
        CONFINED,
        GUARDED
    }
}
