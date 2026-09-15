package com.fablab.rh.repository;

import com.fablab.rh.entity.ApontamentoHoras;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso aos dados da tabela {@code apontamento_horas}.
 */
public interface ApontamentoHorasRepository extends JpaRepository<ApontamentoHoras, Long> {

    List<ApontamentoHoras> findByFuncionarioId(Long funcionarioId);

    @Query("""
            select coalesce(sum(a.horasTrabalhadas), 0)
            from ApontamentoHoras a
            where a.funcionario.id = :funcionarioId
              and a.data = :data
              and a.status in (com.fablab.rh.entity.StatusApontamento.PENDENTE,
                               com.fablab.rh.entity.StatusApontamento.VALIDADO)
            """)
    BigDecimal sumHorasApontadasPorDia(@Param("funcionarioId") Long funcionarioId, @Param("data") LocalDate data);
}