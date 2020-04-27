package com.github.reneweb.stock;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StockClient {
  private static final String URL =
      "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol={symbol}&interval=5min&apikey={apikey}";

  private final String apiKey;
  private final RestTemplate restTemplate;

  @Autowired
  public StockClient(@Value("${ALPHAVANTAGE_APIKEY}") String apiKey, RestTemplate restTemplate) {
    this.apiKey = apiKey;
    this.restTemplate = restTemplate;
  }

  @CircuitBreaker(name = "StockClient")
  @Retry(name = "StockClient")
  public Stock fetchStockBySymbol(String symbol) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<?> request = new HttpEntity<>(headers);

    ResponseEntity<Stock> response =
        restTemplate.exchange(
            URL,
            HttpMethod.GET,
            request,
            Stock.class,
            Map.of(
                "apikey", apiKey,
                "symbol", symbol));

    if (response.getStatusCode().value() == 400) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stock symbol");
    } else if (response.getStatusCode().is4xxClientError()
        || response.getStatusCode().is5xxServerError()) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error while retrieving stock data");
    } else {
      return response.getBody();
    }
  }
}
