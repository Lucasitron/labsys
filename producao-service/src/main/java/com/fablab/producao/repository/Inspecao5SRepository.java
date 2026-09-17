package com.fablab.producao.repository;

import com.fablab.producao.entity.Inspecao5S;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Inspecao5SRepository extends JpaRepository<Inspecao5S, Long> {

    List<Inspecao5S> findBySetor_IdSetorOrderByDataInspecaoDesc(Long idSetor);
}