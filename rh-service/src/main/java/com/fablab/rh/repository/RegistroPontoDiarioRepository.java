package com.fablab.rh.repository;

import com.fablab.rh.entity.RegistroPontoDiario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso aos dados da tabela {@code registro_ponto_diario}.
 */
public interface RegistroPontoDiarioRepository extends JpaRepository<RegistroPontoDiario, Long> {

    Optional<RegistroPontoDiario> findByFuncionarioIdAndData(Long funcionarioId, LocalDate data);

    List<RegistroPontoDiario> findByFuncionarioId(Long funcionarioId);

    @Query("""
            select coalesce(sum(r.totalHoras), 0)
            from RegistroPontoDiario r
            where r.funcionario.id = :funcionarioId and r.data = :data
            """)
    BigDecimal sumTotalHorasPorDia(@Param("funcionarioId") Long funcionarioId, @Param("data") LocalDate data);
}