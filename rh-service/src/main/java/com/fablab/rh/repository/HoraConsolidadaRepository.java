package com.fablab.rh.repository;

import com.fablab.rh.entity.HoraConsolidada;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code hora_consolidada}.
 */
public interface HoraConsolidadaRepository extends JpaRepository<HoraConsolidada, Long> {

    List<HoraConsolidada> findByCertificadoIdCertificado(Long idCertificado);
}
