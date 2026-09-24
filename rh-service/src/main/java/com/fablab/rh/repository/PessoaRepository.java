package com.fablab.rh.repository;

import com.fablab.rh.entity.Pessoa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code pessoa}.
 */
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    Optional<Pessoa> findByMatricula(String matricula);

    boolean existsByMatricula(String matricula);
}