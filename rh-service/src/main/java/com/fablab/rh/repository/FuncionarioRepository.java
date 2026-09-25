package com.fablab.rh.repository;

import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Acesso aos dados da tabela {@code funcionario}.
 */
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByPessoaId(Long pessoaId);

    List<Funcionario> findByNivelAcesso(NivelAcesso nivelAcesso);

    List<Funcionario> findByNivelAcessoNot(NivelAcesso nivelAcesso);

    List<Funcionario> findByDepartamentoIgnoreCase(String departamento);

    boolean existsByPessoaId(Long pessoaId);

    /** Contagem de funcionários por nível de acesso (faceta da listagem). */
    @Query("select f.nivelAcesso, count(f) from Funcionario f group by f.nivelAcesso")
    List<Object[]> contarPorNivel();

    /** Contagem de funcionários por departamento (faceta da listagem). */
    @Query("""
            select f.departamento, count(f) from Funcionario f
            where f.departamento is not null
            group by f.departamento
            """)
    List<Object[]> contarPorDepartamento();
}