package com.chessapp.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

@Table("datasets")
record Dataset(@Id UUID id, String name, String version) {}

interface DatasetRepo extends CrudRepository<Dataset, UUID> {}

public class DatasetRepositoryTest extends DbJdbcTestBase {
  @Autowired DatasetRepo repo;

  @Test
  void createAndRead() {
    var saved = repo.save(new Dataset(null,"it_ds","v1"));
    var loaded = repo.findById(saved.id()).orElseThrow();
    Assertions.assertEquals("it_ds", loaded.name());
  }
}
