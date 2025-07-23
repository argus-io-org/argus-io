package io.argus.starter.aspect;

import io.argus.starter.annotation.ArgusTraceable; // Import your updated annotation
import io.argus.starter.context.TraceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class TracingAspect {

    private static final Logger log = LoggerFactory.getLogger(TracingAspect.class);

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
            log.info("[ARGUS] No active trace found. Initiating new trace with ID: {}", traceId);
        }

        String spanId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().toShortString();

        log.info("[ARGUS] ---> Starting Span [{}] for Method [{}] | TraceID=[{}]", spanId, methodName, traceId);
        long startTime = System.nanoTime();

        try {
            // Execute the actual business logic of the annotated method
            Object result = joinPoint.proceed();

            // No exception
            long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
            log.info("[ARGUS] <--- Finished Span [{}] | Status [SUCCESS] | Duration=[{}ms]", spanId, durationMillis);

            return result;

        } catch (Throwable e) {
            long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
            log.error("[ARGUS] <--- Finished Span [{}] | Status [FAILED] | Error: {} | Duration=[{}ms]", spanId, e.getMessage(), durationMillis);

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