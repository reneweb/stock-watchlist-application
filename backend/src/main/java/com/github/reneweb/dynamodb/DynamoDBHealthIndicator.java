package com.github.reneweb.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DynamoDBHealthIndicator implements HealthIndicator {

  private final AmazonDynamoDB amazonDynamoDB;

  @Autowired
  public DynamoDBHealthIndicator(AmazonDynamoDB amazonDynamoDB) {
    this.amazonDynamoDB = amazonDynamoDB;
  }

  @Override
  public Health health() {
    try {
      amazonDynamoDB.describeTable("Watchlist");
      return Health.up().build();
    } catch (ResourceNotFoundException | InternalServerErrorException e) {
      return Health.down(e).build();
    }
  }
}
