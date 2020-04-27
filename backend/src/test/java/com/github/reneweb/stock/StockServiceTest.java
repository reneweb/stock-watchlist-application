package com.github.reneweb.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.reneweb.facebook.FBAccessTokenValidator;
import com.github.reneweb.facebook.FBTokenData;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.noop.NoopTimer;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {
  @Mock private FBAccessTokenValidator fbAccessTokenValidator;
  @Mock private StockClient stockClient;
  @Mock private StockRepository stockRepository;
  @Mock private MeterRegistry meterRegistry;

  @InjectMocks private StockService stockService;

  private final String userAccessToken = "myUserAccessToken";
  private final String userId = "myUserId";
  private final String symbol = "myStock";
  private final Stock stock = new Stock();

  @Test
  public void shouldGetStock() {
    when(stockClient.fetchStockBySymbol(symbol)).thenReturn(stock);
    Stock stockResult = stockService.getStock(symbol);
    assertThat(stockResult).isEqualTo(stock);
  }

  @Test
  public void shouldGetWatchlist() {
    FBTokenData fbTokenData = new FBTokenData("myAppId", userId, false);
    when(fbAccessTokenValidator.validate(userAccessToken)).thenReturn(fbTokenData);
    when(stockRepository.findById(userId))
        .thenReturn(Optional.of(new StockWatchlistEntity(userId, Set.of(symbol))));
    when(stockClient.fetchStockBySymbol(symbol)).thenReturn(stock);
    mockTimer(true, false, false);

    Set<Stock> stocks = stockService.getWatchlist(userAccessToken);
    assertThat(Set.of(stock)).isEqualTo(stocks);
  }

  @Test
  public void shouldReturnEmptySetIfWatchlistNotFound() {
    FBTokenData fbTokenData = new FBTokenData("myAppId", userId, false);
    when(fbAccessTokenValidator.validate(userAccessToken)).thenReturn(fbTokenData);
    when(stockRepository.findById(userId)).thenReturn(Optional.empty());
    mockTimer(true, false, false);

    Set<Stock> stocks = stockService.getWatchlist(userAccessToken);
    assertThat(Set.of()).isEqualTo(stocks);
  }

  @Test
  public void shouldAddStockToExistingWatchlist() {
    StockWatchlistEntity stockWatchlistEntity = new StockWatchlistEntity(userId, new HashSet<>());
    FBTokenData fbTokenData = new FBTokenData("myAppId", userId, false);
    when(fbAccessTokenValidator.validate(userAccessToken)).thenReturn(fbTokenData);
    when(stockRepository.findById(userId)).thenReturn(Optional.of(stockWatchlistEntity));
    when(stockClient.fetchStockBySymbol(symbol)).thenReturn(stock);
    mockTimer(true, true, false);

    stockService.addStockToWatchlist(userAccessToken, symbol);

    ArgumentCaptor<StockWatchlistEntity> captor =
        ArgumentCaptor.forClass(StockWatchlistEntity.class);
    verify(stockRepository).save(captor.capture());
    assertThat(captor.getValue())
        .isEqualToComparingFieldByFieldRecursively(
            new StockWatchlistEntity(userId, Set.of(symbol)));
  }

  @Test
  public void shouldCreateNewWatchlistWithSymbolIfNotExisting() {
    FBTokenData fbTokenData = new FBTokenData("myAppId", userId, false);
    when(fbAccessTokenValidator.validate(userAccessToken)).thenReturn(fbTokenData);
    when(stockRepository.findById(userId)).thenReturn(Optional.empty());
    when(stockClient.fetchStockBySymbol(symbol)).thenReturn(stock);
    mockTimer(true, true, false);

    stockService.addStockToWatchlist(userAccessToken, symbol);

    ArgumentCaptor<StockWatchlistEntity> captor =
        ArgumentCaptor.forClass(StockWatchlistEntity.class);
    verify(stockRepository).save(captor.capture());
    assertThat(captor.getValue())
        .isEqualToComparingFieldByFieldRecursively(
            new StockWatchlistEntity(userId, Set.of(symbol)));
  }

  @Test
  public void shouldRemoveStockIfWatchlistExists() {
    String anotherSymbol = "anotherSymbol";
    HashSet<String> symbols = new HashSet<>();
    symbols.add(symbol);
    symbols.add(anotherSymbol);
    StockWatchlistEntity stockWatchlistEntity = new StockWatchlistEntity(userId, symbols);
    FBTokenData fbTokenData = new FBTokenData("myAppId", userId, false);
    when(fbAccessTokenValidator.validate(userAccessToken)).thenReturn(fbTokenData);
    when(stockRepository.findById(userId)).thenReturn(Optional.of(stockWatchlistEntity));
    when(stockClient.fetchStockBySymbol(symbol)).thenReturn(stock);
    mockTimer(true, true, false);

    stockService.removeStockFromWatchlist(userAccessToken, symbol);

    ArgumentCaptor<StockWatchlistEntity> captor =
        ArgumentCaptor.forClass(StockWatchlistEntity.class);
    verify(stockRepository).save(captor.capture());
    assertThat(captor.getValue())
        .isEqualToComparingFieldByFieldRecursively(
            new StockWatchlistEntity(userId, Set.of(anotherSymbol)));
  }

  @Test
  public void shouldRemoveEntryIfWatchlistExistsAndEmptyAfterRemovalOperation() {
    HashSet<String> symbols = new HashSet<>();
    symbols.add(symbol);
    StockWatchlistEntity stockWatchlistEntity = new StockWatchlistEntity(userId, symbols);
    FBTokenData fbTokenData = new FBTokenData("myAppId", userId, false);
    when(fbAccessTokenValidator.validate(userAccessToken)).thenReturn(fbTokenData);
    when(stockRepository.findById(userId)).thenReturn(Optional.of(stockWatchlistEntity));
    when(stockClient.fetchStockBySymbol(symbol)).thenReturn(stock);
    mockTimer(true, false, true);

    stockService.removeStockFromWatchlist(userAccessToken, symbol);

    ArgumentCaptor<StockWatchlistEntity> captor =
        ArgumentCaptor.forClass(StockWatchlistEntity.class);
    verify(stockRepository).delete(captor.capture());
    assertThat(captor.getValue())
        .isEqualToComparingFieldByFieldRecursively(new StockWatchlistEntity(userId, Set.of()));
  }

  private void mockTimer(boolean withFindById, boolean withSave, boolean withDelete) {
    Timer timerSupplier = mock(NoopTimer.class);
    lenient().doCallRealMethod().when(timerSupplier).record(any(Supplier.class));

    Timer timerRunnable = mock(NoopTimer.class);
    lenient().doCallRealMethod().when(timerRunnable).record(any(Runnable.class));

    if (withFindById) {
      when(meterRegistry.timer("dynamodb", "operation", "findById")).thenReturn(timerSupplier);
    }
    if (withSave) {
      when(meterRegistry.timer("dynamodb", "operation", "save")).thenReturn(timerSupplier);
    }
    if (withDelete) {
      when(meterRegistry.timer("dynamodb", "operation", "delete")).thenReturn(timerRunnable);
    }
  }
}
