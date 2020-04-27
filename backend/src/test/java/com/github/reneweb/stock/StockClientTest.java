package com.github.reneweb.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StockClientTest {
  @Mock private RestTemplate restTemplate;

  private String apikey = "myApiKey";

  private StockClient stockClient;

  @BeforeEach
  public void setup() {
    stockClient = new StockClient(apikey, restTemplate);
  }

  @Test
  public void shouldReturnStockDataIfCallSuccessful() {
    String symbol = "myStock";
    String url =
        "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol={symbol}&interval=5min&apikey={apikey}";

    Stock stock = new Stock();
    when(restTemplate.exchange(
            eq(url),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Stock.class),
            eq(
                Map.of(
                    "symbol", symbol,
                    "apikey", apikey))))
        .thenReturn(new ResponseEntity<>(stock, HttpStatus.OK));

    Stock stockResult = stockClient.fetchStockBySymbol(symbol);
    assertThat(stockResult).isEqualTo(stock);
  }

  @Test
  public void shouldThrow400ResponseExceptionIfSymbolInvalid() {
    String symbol = "myStock";
    String apikey = "myApiKey";
    String url =
        "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol={symbol}&interval=5min&apikey={apikey}";

    Stock stock = new Stock();
    when(restTemplate.exchange(
            eq(url),
            eq(HttpMethod.GET),
            any(),
            eq(Stock.class),
            eq(
                Map.of(
                    "symbol", symbol,
                    "apikey", apikey))))
        .thenReturn(new ResponseEntity<>(stock, HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> stockClient.fetchStockBySymbol(symbol))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
  }

  @Test
  public void shouldThrowResponseExceptionIfCallUnsuccessful() {
    String symbol = "myStock";
    String apikey = "myApiKey";
    String url =
        "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol={symbol}&interval=5min&apikey={apikey}";

    Stock stock = new Stock();
    when(restTemplate.exchange(
            eq(url),
            eq(HttpMethod.GET),
            any(),
            eq(Stock.class),
            eq(
                Map.of(
                    "symbol", symbol,
                    "apikey", apikey))))
        .thenReturn(new ResponseEntity<>(stock, HttpStatus.FORBIDDEN));

    assertThatThrownBy(() -> stockClient.fetchStockBySymbol(symbol))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
