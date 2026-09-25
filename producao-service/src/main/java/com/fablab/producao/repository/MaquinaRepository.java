package com.fablab.producao.repository;

import com.fablab.producao.entity.Maquina;
import com.fablab.producao.entity.MaquinaStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    List<Maquina> findByStatus(MaquinaStatus status);
}