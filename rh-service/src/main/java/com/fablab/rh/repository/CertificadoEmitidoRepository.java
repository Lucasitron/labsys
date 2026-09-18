package com.fablab.rh.repository;

import com.fablab.rh.entity.CertificadoEmitido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code certificado_emitido}.
 */
public interface CertificadoEmitidoRepository extends JpaRepository<CertificadoEmitido, Long> {

    List<CertificadoEmitido> findAllByOrderByDataEmissaoDesc();

    List<CertificadoEmitido> findByFuncionarioIdOrderByDataEmissaoDesc(Long funcionarioId);
}
