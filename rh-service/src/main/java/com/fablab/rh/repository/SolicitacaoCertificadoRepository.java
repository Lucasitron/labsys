package com.fablab.rh.repository;

import com.fablab.rh.entity.SolicitacaoCertificado;
import com.fablab.rh.entity.StatusSolicitacao;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso aos dados da tabela {@code solicitacao_certificado}.
 */
public interface SolicitacaoCertificadoRepository extends JpaRepository<SolicitacaoCertificado, Long> {

    List<SolicitacaoCertificado> findAllByOrderByDataSolicitacaoDesc();

    List<SolicitacaoCertificado> findByStatusOrderByDataSolicitacaoDesc(StatusSolicitacao status);

    List<SolicitacaoCertificado> findByFuncionarioIdOrderByDataSolicitacaoDesc(Long funcionarioId);

    List<SolicitacaoCertificado> findByFuncionarioIdAndStatusOrderByDataSolicitacaoDesc(
            Long funcionarioId, StatusSolicitacao status);

    /** Obtém a solicitação com bloqueio pessimista, evitando dupla decisão. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitacaoCertificado s where s.idSolicitacao = :id")
    Optional<SolicitacaoCertificado> findByIdForUpdate(@Param("id") Long id);
}
