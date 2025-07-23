package io.argus.starter.instrumentation.http;

import io.argus.common.dto.SpanDto;
import io.argus.starter.context.TraceContextHolder;
import io.argus.starter.publish.SpanPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A ClientHttpRequestInterceptor that instruments RestTemplate calls.
 * It creates a "client span" for the outgoing request and, most importantly,
 * propagates the active Trace ID by injecting it into the HTTP headers.
 */
public class ArgusRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ArgusRestTemplateInterceptor.class);
    public static final String TRACE_ID_HEADER = "X-Argus-Trace-Id";

    private final SpanPublisher spanPublisher;
    private final String serviceName;

    public ArgusRestTemplateInterceptor(SpanPublisher spanPublisher, String serviceName) {
        this.spanPublisher = spanPublisher;
        this.serviceName = serviceName;
    }

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

        Instant startTime = Instant.now();
        try {
            ClientHttpResponse response = execution.execute(request, body);

            Instant endTime = Instant.now();
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

            SpanDto span = SpanDto.builder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .serviceName(serviceName)
                    .methodName(request.getMethod().name() + " " + request.getURI().toString())
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(duration)
                    .status("SUCCESS")
                    .tags(Map.of(
                            "type", "HTTP_CLIENT",
                            "http.url", request.getURI().toString(),
                            "http.status_code", String.valueOf(response.getStatusCode().value())
                    ))
                    .build();

            spanPublisher.publishSpan(span);

            return response;
        } catch (IOException e) {
            Instant endTime = Instant.now();
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

            SpanDto span = SpanDto.builder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .serviceName(serviceName)
                    .methodName(request.getMethod().name() + " " + request.getURI().toString())
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(duration)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .tags(Map.of(
                            "type", "HTTP_CLIENT",
                            "http.url", request.getURI().toString()
                    ))
                    .build();

            spanPublisher.publishSpan(span);

            throw e;
        }
    }
}