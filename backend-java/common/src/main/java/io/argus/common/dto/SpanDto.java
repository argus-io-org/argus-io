package io.argus.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpanDto {
    private String traceId;
    private String spanId;
    private String serviceName;
    private String methodName;
    private Instant startTime;
    private Instant endTime;
    private long durationMs;
    private String status;
    private String errorMessage; // Null if successful
    private Map<String, String> tags;
}