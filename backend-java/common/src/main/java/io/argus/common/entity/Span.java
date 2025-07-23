package io.argus.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "spans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Span {

    @Id
    private String spanId;

    @Column(nullable = false, updatable = false)
    private String traceId;

    private String parentSpanId;

    private String serviceName;

    private String methodName;

    @Column(nullable = false, updatable = false)
    private Instant startTime;

    @Column(nullable = false, updatable = false)
    private Instant endTime;

    private long durationMs;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String tags;
}