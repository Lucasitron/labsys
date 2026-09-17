package com.fablab.producao.repository;

import com.fablab.producao.entity.Setor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorRepository extends JpaRepository<Setor, Long> {

    List<Setor> findByAtivoTrue();
}