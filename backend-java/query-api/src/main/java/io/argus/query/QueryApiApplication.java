package io.argus.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("io.argus.common.entity")
public class QueryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryApiApplication.class, args);
    }
}
