package com.fablab.rh.repository;

import com.fablab.rh.entity.Tutor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code tutor}.
 */
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    Optional<Tutor> findByFuncionarioId(Long funcionarioId);
}