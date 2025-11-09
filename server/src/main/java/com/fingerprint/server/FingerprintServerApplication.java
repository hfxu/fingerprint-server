package com.fingerprint.server;

import com.fingerprint.server.config.FingerprintElasticsearchProperties;
import com.fingerprint.server.config.SimilarityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        FingerprintElasticsearchProperties.class,
        SimilarityProperties.class
})
public class FingerprintServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FingerprintServerApplication.class, args);
    }
}
