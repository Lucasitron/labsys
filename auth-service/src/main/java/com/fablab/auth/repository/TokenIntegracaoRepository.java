package com.fablab.auth.repository;

import com.fablab.auth.entity.TokenIntegracao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Tokens de integração (só hash armazenado, C-5).
 */
public interface TokenIntegracaoRepository extends JpaRepository<TokenIntegracao, Long> {

    List<TokenIntegracao> findByRevogadoFalseOrderByCriadoEmDesc();

    Optional<TokenIntegracao> findByHash(String hash);

    boolean existsByNomeAndRevogadoFalse(String nome);
}
