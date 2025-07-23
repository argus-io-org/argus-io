package io.argus.downstream;

import io.argus.starter.annotation.ArgusTraceable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class DownstreamController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/downstream-hello")
    @ArgusTraceable
    public String sayDownstreamHello() throws InterruptedException {
        // Simulate some work
        Thread.sleep(100);

        // Make the call BACK to service A
        String callbackResponse = restTemplate.getForObject(
                "http://localhost:8080/hello-from-b",
                String.class
        );

        return "Downstream Service says: I have received the callback response -> '" + callbackResponse + "'";
    }
}