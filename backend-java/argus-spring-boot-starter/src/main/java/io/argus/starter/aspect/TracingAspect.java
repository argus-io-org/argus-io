package io.argus.starter.aspect;

import io.argus.common.dto.SpanDto;
import io.argus.starter.annotation.ArgusTraceable;
import io.argus.starter.context.TraceContextHolder;
import io.argus.starter.publish.SpanPublisher;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
public class TracingAspect {

    private static final Logger log = LoggerFactory.getLogger(TracingAspect.class);

    @Autowired
    private SpanPublisher spanPublisher;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Around("@annotation(argusTraceable)")
    public Object trace(ProceedingJoinPoint joinPoint, ArgusTraceable argusTraceable) throws Throwable {
        TraceContext initialContext = TraceContextHolder.getCurrentContext();

        String traceId;
        String parentSpanId;
        boolean isTraceInitiator = (initialContext == null);

        if (isTraceInitiator) {
            traceId = UUID.randomUUID().toString();
            parentSpanId = null; // No parent for a new trace.
            log.debug("[ARGUS] No active trace found. Initiating new trace with ID: {}", traceId);
        } else {
            traceId = initialContext.traceId();
            parentSpanId = initialContext.spanId();
        }

        String spanId = UUID.randomUUID().toString().substring(0, 8);

        TraceContextHolder.startSpan(traceId, spanId);
        Instant startTime = Instant.now();

        try {
            Object result = joinPoint.proceed();
            publishSpan(traceId, spanId, parentSpanId, joinPoint.getSignature().toShortString(), startTime, "SUCCESS", null);
            return result;
        } catch (Throwable e) {
            publishSpan(traceId, spanId, parentSpanId, joinPoint.getSignature().toShortString(), startTime, "FAILED", e.getMessage());
            if (argusTraceable.suppressException()) {
                log.warn("[ARGUS] Exception suppressed by @ArgusTraceable. Returning null.");
                return null;
            } else {
                throw e;
            }
        } finally {
            TraceContextHolder.endSpan();
            if (isTraceInitiator) {
                TraceContextHolder.clear();
                log.debug("[ARGUS] Cleared trace context for trace ID: {}", traceId);
            }
        }
    }

    private void publishSpan(String traceId, String spanId, String parentSpanId, String methodName, Instant startTime, String status, String errorMessage) {
        Instant endTime = Instant.now();
        long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

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
                .tags(Map.of("type", "METHOD"))
                .build();

        spanPublisher.publishSpan(span);
    }
}