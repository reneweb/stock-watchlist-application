package com.github.reneweb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.reneweb.stock.Stock;
import com.github.reneweb.stock.StockWatchlistEntity;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    properties = {
      "ALPHAVANTAGE_APIKEY=alphavantage_apikey",
      "FB_APP_ID=fb_app_id",
      "FB_APP_SECRET=fb_app_secret",
      "DYNAMODB_ENDPOINT=http://localhost:12000",
      "AWS_ACCESSKEY=aws_accesskey",
      "AWS_SECRETKEY=aws_secretkey",
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MainIT {

  @LocalServerPort private int port;

  @Autowired private RestTemplate restTemplate;

  @Autowired private AmazonDynamoDB amazonDynamoDB;

  private MockRestServiceServer mockApiServer;
  private String host;
  private String fbApiJson =
      "{\n"
          + "   \"data\":{\n"
          + "      \"app_id\":\"fb_app_id\",\n"
          + "      \"user_id\":\"%s\"\n"
          + "   }\n"
          + "}";
  private String stockJson =
      "{"
          + "   \"Time Series (5min)\":{"
          + "      \"time\":{"
          + "         \"1. open\":\"1\","
          + "         \"2. high\":\"2\","
          + "         \"3. low\":\"3\","
          + "         \"4. close\":\"4\","
          + "         \"5. volume\":\"5\""
          + "      }"
          + "   }"
          + "}";

  private static DynamoDBProxyServer server;

  @BeforeAll
  public static void setupClass() throws Exception {
    System.setProperty("sqlite4java.library.path", "native-libs");
    String port = "12000";
    server =
        ServerRunner.createServerFromCommandLineArgs(new String[] {"-inMemory", "-port", port});
    server.start();
  }

  @AfterAll
  public static void teardownClass() throws Exception {
    server.stop();
  }

  @BeforeEach
  public void setup() {
    host = String.format("http://localhost:%s", port);
    mockApiServer = MockRestServiceServer.createServer(restTemplate);

    if (!amazonDynamoDB.listTables().getTableNames().contains("Watchlist")) {
      CreateTableRequest tableRequest =
          new DynamoDBMapper(amazonDynamoDB).generateCreateTableRequest(StockWatchlistEntity.class);
      tableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
      amazonDynamoDB.createTable(tableRequest);
    }
  }

  @Test
  public void shouldGetStock() {
    String symbol = "testStock";
    String url = host + "/stocks/" + symbol;
    mockStockApi(symbol);

    Stock stock = new RestTemplate().getForObject(url, Stock.class);
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getOpen())
        .isEqualTo("1");
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getHigh())
        .isEqualTo("2");
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getLow()).isEqualTo("3");
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getClose())
        .isEqualTo("4");
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getVolume())
        .isEqualTo("5");
  }

  @Test
  public void shouldCreateAndGetWatchlist() {
    String symbol = "testStock";
    String userAccessToken = "testAccessToken";
    String userId = "testUserId";
    String postUrl = host + "/watchlist/" + symbol;
    String getUrl = host + "/watchlist";
    mockFbApi(userAccessToken, userId);
    mockStockApi(symbol);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + userAccessToken);
    HttpEntity<?> request = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        new RestTemplate().exchange(postUrl, HttpMethod.POST, request, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Set<Map<String, Object>> watchlist =
        new RestTemplate().exchange(getUrl, HttpMethod.GET, request, Set.class).getBody();
    Stock stock = new ObjectMapper().convertValue(watchlist.iterator().next(), Stock.class);
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getOpen())
        .isEqualTo("1");
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getHigh())
        .isEqualTo("2");
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getLow()).isEqualTo("3");
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getClose())
        .isEqualTo("4");
    assertThat(stock.getStockHistory().values().stream().findFirst().get().getVolume())
        .isEqualTo("5");
  }

  @Test
  public void shouldCreateWatchlistAndDeleteStockFromWatchlist() {
    String symbol = "testStock";
    String userAccessToken = "testAccessToken";
    String userId = "testUserId1";
    String postUrl = host + "/watchlist/" + symbol;
    String deleteUrl = host + "/watchlist/" + symbol;
    String getUrl = host + "/watchlist";
    mockFbApi(userAccessToken, userId);
    mockStockApi(symbol);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + userAccessToken);
    HttpEntity<?> request = new HttpEntity<>(headers);

    ResponseEntity<String> responsePost =
        new RestTemplate().exchange(postUrl, HttpMethod.POST, request, String.class);
    assertThat(responsePost.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> responseDelete =
        new RestTemplate().exchange(deleteUrl, HttpMethod.DELETE, request, String.class);
    assertThat(responseDelete.getStatusCode()).isEqualTo(HttpStatus.OK);

    Set<Map<String, Object>> watchlist =
        new RestTemplate().exchange(getUrl, HttpMethod.GET, request, Set.class).getBody();
    assertThat(watchlist).isEmpty();
  }

  private void mockFbApi(String userAccessToken, String userId) {
    mockApiServer
        .expect(
            ExpectedCount.min(1),
            requestTo(
                String.format(
                    "https://graph.facebook.com/debug_token?input_token=%s&access_token=%s",
                    userAccessToken, "fb_app_id%7Cfb_app_secret")))
        .andRespond(withSuccess(String.format(fbApiJson, userId), MediaType.APPLICATION_JSON));
  }

  private void mockStockApi(String forSymbol) {
    mockApiServer
        .expect(
            ExpectedCount.min(1),
            requestTo(
                String.format(
                    "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=%s&interval=5min&apikey=%s",
                    forSymbol, "alphavantage_apikey")))
        .andRespond(withSuccess(stockJson, MediaType.APPLICATION_JSON));
  }
}
