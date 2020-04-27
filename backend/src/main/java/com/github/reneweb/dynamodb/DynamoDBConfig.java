package com.github.reneweb.dynamodb;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.github.reneweb.stock.StockRepository;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDynamoDBRepositories(basePackageClasses = StockRepository.class)
public class DynamoDBConfig {

  @Value("${DYNAMODB_ENDPOINT:}")
  private String amazonDynamoDbEndpoint;

  @Value("${AWS_ACCESSKEY}")
  private String amazonAWSAccessKey;

  @Value("${AWS_SECRETKEY}")
  private String amazonAWSSecretKey;

  @Bean
  public AmazonDynamoDB amazonDynamoDB() {
    AWSStaticCredentialsProvider awsCredentialsProvider =
        new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey));

    if (!amazonDynamoDbEndpoint.isEmpty()) {
      AwsClientBuilder.EndpointConfiguration endpoint =
          new AwsClientBuilder.EndpointConfiguration(
              amazonDynamoDbEndpoint, Regions.DEFAULT_REGION.getName());
      return AmazonDynamoDBClientBuilder.standard()
          .withCredentials(awsCredentialsProvider)
          .withEndpointConfiguration(endpoint)
          .build();
    } else {
      return AmazonDynamoDBClientBuilder.standard()
          .withCredentials(awsCredentialsProvider)
          .withRegion(Regions.DEFAULT_REGION)
          .build();
    }
  }
}
