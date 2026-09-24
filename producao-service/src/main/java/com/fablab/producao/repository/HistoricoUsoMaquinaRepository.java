package com.fablab.producao.repository;

import com.fablab.producao.entity.HistoricoUsoMaquina;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoUsoMaquinaRepository extends JpaRepository<HistoricoUsoMaquina, Long> {

    List<HistoricoUsoMaquina> findByMaquina_IdMaquinaOrderByDataInicioDesc(Long idMaquina);

    List<HistoricoUsoMaquina> findByDataFimIsNull();
}