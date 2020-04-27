package com.github.reneweb.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class Stock {

  @JsonProperty("Time Series (5min)")
  private Map<String, StockEntry> stockHistory;

  public Map<String, StockEntry> getStockHistory() {
    return stockHistory;
  }
}
