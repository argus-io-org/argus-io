package io.argus.collector.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.argus.common.dto.SpanDto;
import io.argus.common.entity.Span;
import io.argus.collector.repository.SpanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpanConsumer {

    private static final Logger log = LoggerFactory.getLogger(SpanConsumer.class);
    public static final String QUEUE_NAME = "argus.spans.queue";

    @Autowired
    private SpanRepository spanRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @RabbitListener(queues = QUEUE_NAME)
    public void receiveSpan(String jsonPayload) {
        log.debug("Received raw JSON payload: {}", jsonPayload);

        try {
            SpanDto spanDto = objectMapper.readValue(jsonPayload, SpanDto.class);

            log.info("Successfully deserialized span: {}", spanDto.getSpanId());

            Span spanEntity = Span.builder()
                    .spanId(spanDto.getSpanId())
                    .traceId(spanDto.getTraceId())
                    .parentSpanId(spanDto.getParentSpanId())
                    .serviceName(spanDto.getServiceName())
                    .methodName(spanDto.getMethodName())
                    .startTime(spanDto.getStartTime())
                    .endTime(spanDto.getEndTime())
                    .durationMs(spanDto.getDurationMs())
                    .status(spanDto.getStatus())
                    .errorMessage(spanDto.getErrorMessage())
                    .tags(objectMapper.writeValueAsString(spanDto.getTags()))
                    .build();

            spanRepository.save(spanEntity);
            log.info("Successfully saved span {} to the database.", spanEntity.getSpanId());

        } catch (Exception e) {
            log.error("Failed to process span message. Payload: {}. Error: {}", jsonPayload, e.getMessage(), e);
        }
    }
}