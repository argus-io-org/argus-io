package io.argus.starter.filter;

import io.argus.starter.context.TraceContextHolder;
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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ArgusTraceFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ArgusTraceFilter.class);
    private static final String TRACE_ID_HEADER = "X-Argus-Trace-Id";
    private static final String PARENT_SPAN_ID_HEADER = "X-Argus-Parent-Span-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        String parentSpanId = httpRequest.getHeader(PARENT_SPAN_ID_HEADER);

        boolean isNewTrace = (traceId == null);
        if (isNewTrace) {
            traceId = UUID.randomUUID().toString();
            parentSpanId = null;
            log.debug("[ARGUS] No trace header found. Initiating new trace with ID: {}", traceId);
        } else {
            log.debug("[ARGUS] Continuing trace with ID from header: {}", traceId);
        }

        TraceContextHolder.setInitialContext(traceId, parentSpanId);

        try {
            chain.doFilter(request, response);
        } finally {
            TraceContextHolder.clear();
            log.debug("[ARGUS] Cleared trace context for trace ID: {}", traceId);
        }
    }
}