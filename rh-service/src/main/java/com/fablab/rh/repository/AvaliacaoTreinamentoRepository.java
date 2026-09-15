package com.fablab.rh.repository;

import com.fablab.rh.entity.AvaliacaoTreinamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code avaliacao_treinamento}.
 */
public interface AvaliacaoTreinamentoRepository extends JpaRepository<AvaliacaoTreinamento, Long> {

    List<AvaliacaoTreinamento> findByTreinamentoId(Long treinamentoId);
}