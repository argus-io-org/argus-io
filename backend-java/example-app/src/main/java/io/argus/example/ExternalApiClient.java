package io.argus.example;

import io.argus.starter.annotation.ArgusTraceable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ExternalApiClient {

    @Autowired
    private RestTemplate restTemplate;

    @ArgusTraceable(suppressException = true)
    public String callExternalApi(){
        return restTemplate.getForObject(
                "https://api.publicapis.org/random",
                String.class
        );
    }
}
