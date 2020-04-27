package com.github.reneweb.stock;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import java.util.Set;

@DynamoDBTable(tableName = "Watchlist")
public class StockWatchlistEntity {
  private String userId;
  private Set<String> symbols;

  public StockWatchlistEntity() {}

  public StockWatchlistEntity(String userId, Set<String> symbols) {
    this.userId = userId;
    this.symbols = symbols;
  }

  @DynamoDBHashKey
  public String getUserId() {
    return userId;
  }

  @DynamoDBAttribute
  public Set<String> getSymbols() {
    return symbols;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public void setSymbols(Set<String> symbols) {
    this.symbols = symbols;
  }
}
