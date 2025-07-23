package io.argus.starter.instrumentation.http;

import io.argus.common.dto.SpanDto;
import io.argus.starter.context.TraceContextHolder;
import io.argus.starter.publish.SpanPublisher;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ArgusRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    public static final String TRACE_ID_HEADER = "X-Argus-Trace-Id";
    public static final String PARENT_SPAN_ID_HEADER = "X-Argus-Parent-Span-Id"; // <-- New Header

    private final SpanPublisher spanPublisher;
    private final String serviceName;

    public ArgusRestTemplateInterceptor(SpanPublisher spanPublisher, String serviceName) {
        this.spanPublisher = spanPublisher;
        this.serviceName = serviceName;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        String traceId = TraceContextHolder.getTraceId();
        String parentSpanId = TraceContextHolder.getSpanId();

        if (traceId == null || parentSpanId == null) {
            return execution.execute(request, body);
        }

        request.getHeaders().add(TRACE_ID_HEADER, traceId);
        request.getHeaders().add(PARENT_SPAN_ID_HEADER, parentSpanId);

        String spanId = UUID.randomUUID().toString().substring(0, 8);
        Instant startTime = Instant.now();

        try {
            ClientHttpResponse response = execution.execute(request, body);
            publishSpan(traceId, spanId, parentSpanId, request, startTime, "SUCCESS", null, String.valueOf(response.getStatusCode().value()));
            return response;
        } catch (IOException e) {
            publishSpan(traceId, spanId, parentSpanId, request, startTime, "FAILED", e.getMessage(), null);
            throw e;
        }
    }

    private void publishSpan(String traceId, String spanId, String parentSpanId, HttpRequest request, Instant startTime, String status, String errorMessage, String httpStatusCode) {
        Instant endTime = Instant.now();
        long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

        String methodName = request.getMethod().name() + " " + request.getURI().toString();

        Map<String, String> tags = new java.util.HashMap<>();
        tags.put("type", "HTTP_CLIENT");
        tags.put("http.url", request.getURI().toString());
        if (httpStatusCode != null) {
            tags.put("http.status_code", httpStatusCode);
        }

        SpanDto span = SpanDto.builder()
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .serviceName(serviceName)
                .methodName(methodName)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(duration)
                .status(status)
                .errorMessage(errorMessage)
                .tags(tags)
                .build();

        spanPublisher.publishSpan(span);
    }
}