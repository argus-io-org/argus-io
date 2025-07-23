package io.argus.starter.aspect;

import io.argus.starter.annotation.ArgusTraceable; // Import your updated annotation
import io.argus.starter.context.TraceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.argus.starter.publish.SpanPublisher;
import io.argus.common.dto.SpanDto;

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

    /*
    This aspect makes sure that for any method annotated with the annotation, we either have a trace existing from a HTTP request
    or we create a new traceID starting from here
     */
    @Around("@annotation(argusTraceable)")
    public Object trace(ProceedingJoinPoint joinPoint, ArgusTraceable argusTraceable) throws Throwable { // Note the new arguments
        String traceId = TraceContextHolder.getTraceId();
        boolean isTraceInitiator = false;

        if (traceId == null) {
            // We need to create a new traceId
            traceId = UUID.randomUUID().toString();
            TraceContextHolder.setTraceId(traceId);
            isTraceInitiator = true;
            log.debug("[ARGUS] No active trace found. Initiating new trace with ID: {}", traceId);
        }

        String spanId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().toShortString();

        log.info("[ARGUS] ---> Starting Span [{}] for Method [{}] | TraceID=[{}]", spanId, methodName, traceId);
        Instant startTime = Instant.now();

        try {
            // Execute the actual business logic of the annotated method
            Object result = joinPoint.proceed();

            // No exception
            Instant endTime = Instant.now();
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

            SpanDto span = SpanDto.builder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .serviceName(serviceName)
                    .methodName(joinPoint.getSignature().toShortString())
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(duration)
                    .status("SUCCESS")
                    .tags(Map.of("type", "METHOD"))
                    .build();

            spanPublisher.publishSpan(span);

            return result;

        } catch (Throwable e) {
            Instant endTime = Instant.now();
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

            SpanDto span = SpanDto.builder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .serviceName(serviceName)
                    .methodName(joinPoint.getSignature().toShortString())
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(duration)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .tags(Map.of("type", "METHOD"))
                    .build();

            spanPublisher.publishSpan(span);

            // Now, check the annotation to see if we should suppress this exception
            if (argusTraceable.suppressException()) {
                log.warn("[ARGUS] Exception suppressed by @ArgusTraceable for method [{}]. Returning null.", methodName);
                return null;
            } else {
                throw e;
            }
        } finally {
            if (isTraceInitiator) {
                TraceContextHolder.clear();
                log.debug("[ARGUS] Cleared trace context for trace ID: {}", traceId);
            }
        }
    }
}