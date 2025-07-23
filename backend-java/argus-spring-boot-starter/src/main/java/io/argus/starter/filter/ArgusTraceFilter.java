package io.argus.starter.filter;

import io.argus.starter.context.TraceContextHolder;
import io.argus.starter.publish.SpanPublisher;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * A servlet filter that is the entry point for all incoming HTTP requests.
 * Its job is to establish the trace context by either adopting an existing
 * Trace ID from request headers or creating a new one.
 * It also ensures the context is cleared after the request is complete.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Ensure this filter runs first
public class ArgusTraceFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ArgusTraceFilter.class);
    private static final String TRACE_ID_HEADER = "X-Argus-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 1. Attempt to get the Trace ID from the incoming request header.
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);

        if (traceId == null) {
            // 2. If no header is present, this is the start of a new trace.
            // Generate a brand new Trace ID.
            traceId = UUID.randomUUID().toString();
            log.info("[ARGUS] No trace header found. Initiating new trace with ID: {}", traceId);
        } else {
            // 3. If a header is present, we are continuing an existing trace.
            log.info("[ARGUS] Continuing trace with ID from header: {}", traceId);
        }

        // 4. Set the determined Trace ID in our ThreadLocal context.
        TraceContextHolder.setTraceId(traceId);

        try {
            // 5. CRITICAL: Pass the request down the filter chain to the next filter or the controller.
            chain.doFilter(request, response);
        } finally {
            // 6. CRITICAL: After the entire request has been processed, clear the ThreadLocal.
            // This prevents memory leaks and trace ID bleed-over in a pooled thread environment.
            TraceContextHolder.clear();
            log.debug("[ARGUS] Cleared trace context for trace ID: {}", traceId);
        }
    }
}