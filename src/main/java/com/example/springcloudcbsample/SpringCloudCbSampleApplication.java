package com.example.springcloudcbsample;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class SpringCloudCbSampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpringCloudCbSampleApplication.class, args);
  }

  @Bean
  public Customizer<Resilience4JCircuitBreakerFactory> specificCustomConfiguration() {

    var timeLimiterConfig = TimeLimiterConfig.custom()
        .timeoutDuration(Duration.ofSeconds(4))
        .build();
    var circuitBreakerConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofMillis(1000))
        .slidingWindowSize(2)
        .build();

    return factory -> factory.configure(
        builder -> builder.circuitBreakerConfig(circuitBreakerConfig)
            .timeLimiterConfig(timeLimiterConfig).build(), "myCircuitBreaker");
  }
}

@RestController
class AlbumController {

  private final AlbumService albumService;

  AlbumController(AlbumService albumService) {
    this.albumService = albumService;
  }

  @GetMapping("/albums")
  String albums() {
    return albumService.getAlbumList();
  }
}

@Service
@SuppressWarnings("rawtypes")
class AlbumService {

  private final CircuitBreakerFactory circuitBreakerFactory;

  private final RestTemplate restTemplate = new RestTemplate();

  public AlbumService(CircuitBreakerFactory circuitBreakerFactory) {
    this.circuitBreakerFactory = circuitBreakerFactory;
  }

  public String getAlbumList() {
    var circuitBreaker = circuitBreakerFactory.create("myCircuitBreaker");
    String url = "https://jsonplaceholder.typicode.com/albums";
//    String fakeUrl = "https://jsonplaceholder.typicode.com/albums-fake";

    return circuitBreaker.run(() -> restTemplate.getForObject(url, String.class),
        throwable -> getDefaultAlbumList());
  }

  private String getDefaultAlbumList() {
    return "[ { \"userId\": 1, \"id\": 1, \"title\": \"default album\" } ]";
  }
}
