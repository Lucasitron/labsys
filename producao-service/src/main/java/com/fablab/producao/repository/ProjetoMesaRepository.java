package com.fablab.producao.repository;

import com.fablab.producao.entity.ProjetoMesa;
import com.fablab.producao.entity.StatusProjetoMesa;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetoMesaRepository extends JpaRepository<ProjetoMesa, Long> {

    List<ProjetoMesa> findByStatus(StatusProjetoMesa status);

    List<ProjetoMesa> findByStatusAndDataUltimaEvolucaoBefore(StatusProjetoMesa status, LocalDate data);
}