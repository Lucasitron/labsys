package com.fablab.rh.repository;

import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso aos dados da tabela {@code pessoa}.
 */
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    Optional<Pessoa> findByMatricula(String matricula);

    boolean existsByMatricula(String matricula);

    boolean existsByCpf(String cpf);

    Optional<Pessoa> findByCpf(String cpf);

    /**
     * Lista paginada com busca textual (nome/matrícula/contato) e filtros por
     * status, nível de acesso e departamento (via vínculo de funcionário).
     */
    @Query("""
            select p from Pessoa p left join Funcionario f on f.pessoa.id = p.id
            where (:busca is null
                   or lower(p.nomeCompleto) like lower(concat('%', :busca, '%'))
                   or lower(p.matricula) like lower(concat('%', :busca, '%'))
                   or lower(p.contato) like lower(concat('%', :busca, '%')))
              and (:status is null or p.status = :status)
              and (:nivel is null or f.nivelAcesso = :nivel)
              and (:departamento is null or lower(f.departamento) = lower(:departamento))
            order by p.nomeCompleto asc, p.id asc
            """)
    Page<Pessoa> buscarComFiltros(@Param("busca") String busca,
                                  @Param("status") PessoaStatus status,
                                  @Param("nivel") NivelAcesso nivel,
                                  @Param("departamento") String departamento,
                                  Pageable pageable);

    /** Contagem de pessoas por status (respeitando a busca textual). */
    @Query("""
            select p.status, count(p) from Pessoa p
            where (:busca is null
                   or lower(p.nomeCompleto) like lower(concat('%', :busca, '%'))
                   or lower(p.matricula) like lower(concat('%', :busca, '%'))
                   or lower(p.contato) like lower(concat('%', :busca, '%')))
            group by p.status
            """)
    List<Object[]> contarPorStatus(@Param("busca") String busca);
}