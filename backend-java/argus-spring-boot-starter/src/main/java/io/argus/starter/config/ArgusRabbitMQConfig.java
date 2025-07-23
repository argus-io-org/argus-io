package io.argus.starter.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArgusRabbitMQConfig {

    public static final String QUEUE_NAME = "argus.spans.queue";

    @Bean
    public Queue spanQueue() {
        return new Queue(QUEUE_NAME, true);
    }
}