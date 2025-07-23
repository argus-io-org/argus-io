package io.argus.starter.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.argus.common.dto.SpanDto;
import io.argus.starter.config.ArgusRabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpanPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpanPublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public void publishSpan(SpanDto span) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(span);
            rabbitTemplate.convertAndSend(ArgusRabbitMQConfig.QUEUE_NAME, jsonPayload);

            log.trace("Successfully published span {} to RabbitMQ.", span.getSpanId());
        } catch (Exception e) {
            log.error("Failed to publish span to RabbitMQ. Error: {}", e.getMessage());
        }
    }
}