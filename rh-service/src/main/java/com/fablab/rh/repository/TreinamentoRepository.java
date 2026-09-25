package com.fablab.rh.repository;

import com.fablab.rh.entity.Treinamento;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code treinamento}.
 */
public interface TreinamentoRepository extends JpaRepository<Treinamento, Long> {
}