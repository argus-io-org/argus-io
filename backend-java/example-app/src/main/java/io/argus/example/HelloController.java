package io.argus.example;

import io.argus.starter.annotation.ArgusTraceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ExternalApiClient externalApiClient;

    @GetMapping("/hello")
    @ArgusTraceable
    public String sayHello() {
        log.info("Phase 1: Calling our internal downstream service...");

        // --- First HTTP Call (to our own instrumented service) ---
        String downstreamResponse = restTemplate.getForObject(
                "http://localhost:8081/downstream-hello",
                String.class
        );

        log.info("Phase 2: Calling a third-party external API...");

        String externalApiResponse = externalApiClient.callExternalApi();

        log.info("Phase 3: Assembling final response.");

        return "Upstream says: '" + downstreamResponse + "' and the external API says something about: '" + externalApiResponse;
    }

    @GetMapping("/hello-from-b")
    @ArgusTraceable
    public String helloFromB() {
        return "HelloController acknowledges the call from B!";
    }
}