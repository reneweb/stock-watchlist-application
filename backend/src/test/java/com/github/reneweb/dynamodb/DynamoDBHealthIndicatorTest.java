package com.github.reneweb.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@ExtendWith(MockitoExtension.class)
class DynamoDBHealthIndicatorTest {

  @Mock private AmazonDynamoDB amazonDynamoDB;

  @InjectMocks private DynamoDBHealthIndicator dynamoDBHealthIndicator;

  @Test
  public void shouldBeUpIfTableCanBeDescribed() {
    when(amazonDynamoDB.describeTable("Watchlist")).thenReturn(null);
    Health health = dynamoDBHealthIndicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  public void shouldBeDownIfProblemWithServer() {
    when(amazonDynamoDB.describeTable("Watchlist"))
        .thenThrow(new InternalServerErrorException("Server Error"));
    Health health = dynamoDBHealthIndicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void shouldBeDownIfTableNotFound() {
    when(amazonDynamoDB.describeTable("Watchlist"))
        .thenThrow(new ResourceNotFoundException("Not found"));
    Health health = dynamoDBHealthIndicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
  }
}
