package com.fablab.estoque.repository;

import com.fablab.estoque.entity.Emprestimo;
import com.fablab.estoque.entity.StatusEmprestimo;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de empréstimos.
 */
public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    List<Emprestimo> findByStatusInAndDataDevolucaoPrevistaBefore(
            Collection<StatusEmprestimo> statuses, LocalDate data);

    /** Empréstimos de uma pessoa (escopo do tomador). */
    List<Emprestimo> findByIdPessoa(Long idPessoa);

    /** Empréstimos por status. */
    List<Emprestimo> findByStatusIn(Collection<StatusEmprestimo> statuses);
}