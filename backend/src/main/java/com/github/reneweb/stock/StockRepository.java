package com.github.reneweb.stock;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface StockRepository extends CrudRepository<StockWatchlistEntity, String> {}
