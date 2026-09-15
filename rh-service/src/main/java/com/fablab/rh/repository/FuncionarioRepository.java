package com.fablab.rh.repository;

import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code funcionario}.
 */
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByPessoaId(Long pessoaId);

    List<Funcionario> findByNivelAcesso(NivelAcesso nivelAcesso);

    List<Funcionario> findByDepartamentoIgnoreCase(String departamento);

    boolean existsByPessoaId(Long pessoaId);
}