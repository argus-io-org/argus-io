package io.argus.starter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for tracing by the Argus system.
 * This will automatically create a "span" for the method's execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArgusTraceable {

    /**
     * If true, any exception thrown by this method will be caught and suppressed.
     * The method will return null (or a default value for primitives) instead.
     * This provides automatic fault tolerance.
     * Defaults to false.
     */
    boolean suppressException() default false;
}
