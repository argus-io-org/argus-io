package io.argus.starter.instrumentation.http;

import io.argus.starter.context.TraceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * A ClientHttpRequestInterceptor that instruments RestTemplate calls.
 * It creates a "client span" for the outgoing request and, most importantly,
 * propagates the active Trace ID by injecting it into the HTTP headers.
 */
public class ArgusRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ArgusRestTemplateInterceptor.class);
    public static final String TRACE_ID_HEADER = "X-Argus-Trace-Id";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // 1. Check if we are currently inside an active trace.
        String traceId = TraceContextHolder.getTraceId();
        if (traceId == null) {
            // If not in a trace, we do nothing. The request proceeds as normal.
            return execution.execute(request, body);
        }

        // 2. We are in a trace! Let's propagate the context.
        // THIS IS THE MOST IMPORTANT LINE: it passes the trace to the next service.
        request.getHeaders().add(TRACE_ID_HEADER, traceId);

        // 3. Create a new span for this specific HTTP client call.
        String spanId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[ARGUS] ---> Starting HTTP Client Span [{}] | {} {} | TraceID=[{}]",
                spanId, request.getMethod(), request.getURI(), traceId);

        long startTime = System.nanoTime();
        try {
            // 4. Execute the actual HTTP request.
            ClientHttpResponse response = execution.execute(request, body);

            // 5. Log the successful result after execution.
            long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
            log.info("[ARGUS] <--- Finished HTTP Client Span [{}] | Status [{}] | Duration=[{}ms]",
                    spanId, response.getStatusCode(), durationMillis);

            return response;
        } catch (IOException e) {
            // 6. Also log errors if the request fails.
            long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
            log.error("[ARGUS] <--- Failed HTTP Client Span [{}] | Error: {} | Duration=[{}ms]",
                    spanId, e.getMessage(), durationMillis);
            throw e; // Re-throw the exception so the calling code knows it failed.
        }
    }
}