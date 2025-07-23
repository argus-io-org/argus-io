package io.argus.starter.config;

import io.argus.starter.aspect.TracingAspect;
import io.argus.starter.filter.ArgusTraceFilter;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import io.argus.starter.instrumentation.http.ArgusRestTemplateInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Spring Boot Auto-Configuration to automatically enable the Argus tracing aspect and trace filter.
 */
@Configuration
public class ArgusAutoConfiguration {

    @Autowired(required = false)
    private List<RestTemplate> restTemplates;

    @Bean
    public TracingAspect tracingAspect() {
        return new TracingAspect();
    }

    @Bean
    public ArgusTraceFilter argusTraceFilter() {
        return new ArgusTraceFilter();
    }

    @PostConstruct
    public void addRestTemplateInterceptor() {
        if (restTemplates != null) {
            restTemplates.forEach(restTemplate -> {
                restTemplate.getInterceptors().add(new ArgusRestTemplateInterceptor());
            });
        }
    }
}