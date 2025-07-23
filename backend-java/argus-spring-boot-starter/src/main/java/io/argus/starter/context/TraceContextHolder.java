package io.argus.starter.context;

import io.argus.starter.aspect.TraceContext;

import java.util.ArrayDeque;
import java.util.Deque;

public class TraceContextHolder {

    private static final ThreadLocal<Deque<TraceContext>> CONTEXT_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Starts a new span by pushing its context onto the stack.
     * @param traceId The trace ID.
     * @param spanId The new span's ID.
     */
    public static void startSpan(String traceId, String spanId) {
        CONTEXT_STACK.get().push(new TraceContext(traceId, spanId));
    }

    /**
     * Ends the current span by popping its context from the stack.
     * This restores the parent's context as the current one.
     */
    public static void endSpan() {
        if (!CONTEXT_STACK.get().isEmpty()) {
            CONTEXT_STACK.get().pop();
        }
    }

    /**
     * Gets the Trace ID of the currently active trace.
     * @return The Trace ID, or null if no trace is active.
     */
    public static String getTraceId() {
        TraceContext current = CONTEXT_STACK.get().peek();
        return (current != null) ? current.traceId() : null;
    }

    /**
     * Gets the Span ID of the currently active span.
     * @return The current Span ID, or null if no span is active.
     */
    public static String getSpanId() {
        TraceContext current = CONTEXT_STACK.get().peek();
        return (current != null) ? current.spanId() : null;
    }

    /**
     * Clears the entire trace context for the current thread.
     * Called by the filter at the end of a request.
     */
    public static void clear() {
        CONTEXT_STACK.get().clear();
    }

    /**
     * A special method for the entry-point filter to set the initial context,
     * which includes the parent span from the HTTP header.
     * This context has a null spanId, signaling that it's a "pre-context"
     * for the first real span to be created by the TracingAspect.
     * @param traceId The trace ID.
     * @param parentSpanId The parent span ID from the header.
     */
    public static void setInitialContext(String traceId, String parentSpanId) {
        CONTEXT_STACK.get().push(new TraceContext(traceId, parentSpanId));
    }

    /**
     * Gets the full context (traceId and spanId) of the currently active span.
     * @return The current TraceContext, or null if no span is active.
     */
    public static TraceContext getCurrentContext() {
        return CONTEXT_STACK.get().peek();
    }
}