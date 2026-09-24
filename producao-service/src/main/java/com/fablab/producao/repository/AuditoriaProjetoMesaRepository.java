package com.fablab.producao.repository;

import com.fablab.producao.entity.AuditoriaProjetoMesa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaProjetoMesaRepository extends JpaRepository<AuditoriaProjetoMesa, Long> {

    List<AuditoriaProjetoMesa> findByProjetoMesa_IdProjetoMesa(Long idProjetoMesa);
}