package io.argus.starter.aspect;

/**
 * The TraceContext record holds the essential information for tracing:
 * @param traceId the ID of the current trace
 * @param spanId the ID of the current span
 */
public record TraceContext(String traceId, String spanId) {}
