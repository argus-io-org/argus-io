package io.argus.starter.context;

public class TraceContextHolder {

    // Ensure that the trace ID is stored in a ThreadLocal variable
    // Each thread can only see its own trace ID
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
