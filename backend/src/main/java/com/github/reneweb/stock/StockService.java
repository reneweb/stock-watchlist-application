package com.github.reneweb.stock;

import com.github.reneweb.facebook.FBAccessTokenValidator;
import com.github.reneweb.facebook.FBTokenData;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockService {

  private final FBAccessTokenValidator fbAccessTokenValidator;
  private final StockClient stockClient;
  private final StockRepository stockRepository;
  private final MeterRegistry meterRegistry;

  @Autowired
  public StockService(
      FBAccessTokenValidator fbAccessTokenValidator,
      StockClient stockClient,
      StockRepository stockRepository,
      MeterRegistry meterRegistry) {
    this.fbAccessTokenValidator = fbAccessTokenValidator;
    this.stockClient = stockClient;
    this.stockRepository = stockRepository;
    this.meterRegistry = meterRegistry;
  }

  public Stock getStock(String symbol) {
    return stockClient.fetchStockBySymbol(symbol);
  }

  public Set<Stock> getWatchlist(String userAccessToken) {
    FBTokenData tokenData = fbAccessTokenValidator.validate(userAccessToken);

    return timedFindById(tokenData)
        .map(
            entity ->
                entity.getSymbols().stream()
                    .map(stockClient::fetchStockBySymbol)
                    .collect(Collectors.toSet()))
        .orElse(Set.of());
  }

  public void addStockToWatchlist(String userAccessToken, String symbol) {
    FBTokenData tokenData = fbAccessTokenValidator.validate(userAccessToken);
    stockClient.fetchStockBySymbol(symbol);

    timedFindById(tokenData)
        .ifPresentOrElse(
            entity -> {
              entity.getSymbols().add(symbol);
              timedSave(entity);
            },
            () -> {
              StockWatchlistEntity entity =
                  new StockWatchlistEntity(tokenData.getUserId(), Set.of(symbol));
              timedSave(entity);
            });
  }

  public void removeStockFromWatchlist(String userAccessToken, String symbol) {
    FBTokenData tokenData = fbAccessTokenValidator.validate(userAccessToken);
    stockClient.fetchStockBySymbol(symbol);

    timedFindById(tokenData)
        .ifPresent(
            entity -> {
              entity.getSymbols().remove(symbol);
              if (entity.getSymbols().isEmpty()) {
                timedDelete(entity);
              } else {
                timedSave(entity);
              }
            });
  }

  private Optional<StockWatchlistEntity> timedFindById(FBTokenData tokenData) {
    return meterRegistry
        .timer("dynamodb", "operation", "findById")
        .record(() -> stockRepository.findById(tokenData.getUserId()));
  }

  private void timedSave(StockWatchlistEntity entity) {
    meterRegistry.timer("dynamodb", "operation", "save").record(() -> stockRepository.save(entity));
  }

  private void timedDelete(StockWatchlistEntity entity) {
    meterRegistry
        .timer("dynamodb", "operation", "delete")
        .record(() -> stockRepository.delete(entity));
  }
}
