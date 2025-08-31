package com.chessapp.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

@Table("games")
record Game(@Id UUID id, String platform, String game_id_ext) {}

interface GameRepo extends CrudRepository<Game, UUID> {}

public class GameRepositoryTest extends DbJdbcTestBase {
  @Autowired GameRepo repo;

  @Test
  void seedPresent() {
    var any = repo.findAll().iterator().hasNext();
    Assertions.assertTrue(any, "Expected at least one seeded game");
  }
}
