package com.github.reneweb.facebook;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FBAccessTokenValidator {
  private static final String URL =
      "https://graph.facebook.com/debug_token?input_token={token}&access_token={appToken}";

  private final String appId;
  private final String appSecret;
  private final RestTemplate restTemplate;

  @Autowired
  public FBAccessTokenValidator(
      @Value("${FB_APP_ID}") String appId,
      @Value("${FB_APP_SECRET}") String appSecret,
      RestTemplate restTemplate) {
    this.appId = appId;
    this.appSecret = appSecret;
    this.restTemplate = restTemplate;
  }

  @CircuitBreaker(name = "FbClient")
  @Retry(name = "FbClient")
  public FBTokenData validate(String userAccessToken) {
    ResponseEntity<FBTokenData> fbResponse =
        restTemplate.getForEntity(
            URL,
            FBTokenData.class,
            Map.of("token", userAccessToken, "appToken", String.format("%s|%s", appId, appSecret)));

    FBTokenData data = fbResponse.getBody();
    if (!data.hasError()) {
      return data;
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
    }
  }
}
