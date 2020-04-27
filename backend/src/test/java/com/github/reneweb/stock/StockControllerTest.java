package com.github.reneweb.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockControllerTest {
  @Mock private StockService stockService;

  @InjectMocks private StockController stockController;

  @Test
  public void getStockShouldDelegateToService() {
    String symbol = "myStock";
    Stock stock = new Stock();
    when(stockService.getStock(symbol)).thenReturn(stock);
    Stock stockResult = stockController.getStock(symbol);
    assertThat(stockResult).isEqualTo(stock);
  }

  @Test
  public void addStockToWatchlistShouldExtractAuthHeaderAndDelegateToService() {
    String authHeader = "Bearer 123";
    String symbol = "myStock";
    stockController.addStockToWatchlist(authHeader, symbol);

    verify(stockService).addStockToWatchlist("123", symbol);
  }

  @Test
  public void deleteStockFromWatchlistShouldExtractAuthHeaderAndDelegateToService() {
    String authHeader = "Bearer 123";
    String symbol = "myStock";
    stockController.deleteStockFromWatchlist(authHeader, symbol);

    verify(stockService).removeStockFromWatchlist("123", symbol);
  }
}
