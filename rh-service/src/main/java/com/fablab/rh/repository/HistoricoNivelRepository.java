package com.fablab.rh.repository;

import com.fablab.rh.entity.HistoricoNivel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code historico_nivel}.
 */
public interface HistoricoNivelRepository extends JpaRepository<HistoricoNivel, Long> {

    List<HistoricoNivel> findByFuncionarioId(Long funcionarioId);
}