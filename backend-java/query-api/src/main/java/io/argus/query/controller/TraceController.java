package io.argus.query.controller;

import io.argus.common.entity.Span;
import io.argus.query.repository.SpanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traces")
@CrossOrigin(origins = "http://localhost:4200")
public class TraceController {

    @Autowired
    private SpanRepository spanRepository;

    /**
     * Finds and returns all spans associated with a given trace ID.
     * @param traceId The ID of the trace to retrieve.
     * @return A list of spans belonging to the trace.
     */
    @GetMapping("/{traceId}")
    public List<Span> getTraceById(@PathVariable String traceId) {
        return spanRepository.findByTraceIdOrderByStartTimeAsc(traceId);
    }
}