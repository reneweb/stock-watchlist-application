package com.github.reneweb.stock;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StockController {

  private final StockService stockService;

  @Autowired
  public StockController(StockService stockService) {
    this.stockService = stockService;
  }

  @GetMapping("/stocks/{symbol}")
  public Stock getStock(@PathVariable String symbol) {
    return stockService.getStock(symbol);
  }

  @GetMapping("/watchlist")
  public Set<Stock> getWatchlist(@RequestHeader("Authorization") String authorizationHeader) {
    return stockService.getWatchlist(getTokenFromHeader(authorizationHeader));
  }

  @PostMapping("/watchlist/{symbol}")
  public void addStockToWatchlist(
      @RequestHeader("Authorization") String authorizationHeader, @PathVariable String symbol) {
    stockService.addStockToWatchlist(getTokenFromHeader(authorizationHeader), symbol);
  }

  @DeleteMapping("/watchlist/{symbol}")
  public void deleteStockFromWatchlist(
      @RequestHeader("Authorization") String authorizationHeader, @PathVariable String symbol) {
    stockService.removeStockFromWatchlist(getTokenFromHeader(authorizationHeader), symbol);
  }

  private String getTokenFromHeader(String authorizationHeader) {
    String[] authHeaderSplit = authorizationHeader.split(" ");
    if (authHeaderSplit.length < 2) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Authorization Header");
    }

    return authHeaderSplit[1];
  }
}
