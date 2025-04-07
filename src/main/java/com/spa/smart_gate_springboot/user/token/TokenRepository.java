package com.spa.smart_gate_springboot.user.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {

  @Query(value = """
      select t from Token t inner join User u\s
      on t.usrId = u.id\s
      where u.id = :id and (t.expired = false or t.revoked = false)\s
      """)
  List<Token> findAllValidTokenByUser(UUID id);

  Optional<Token> findByToken(String token);
  Optional<Token> findByTokenAndRevokedAndExpired(String token,boolean revoked,boolean expired);
  void deleteByRevokedTrueAndExpiredTrue();
}
