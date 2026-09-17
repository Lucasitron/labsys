package com.fablab.producao.repository;

import com.fablab.producao.entity.Projeto;
import com.fablab.producao.entity.ProjetoStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    List<Projeto> findByStatus(ProjetoStatus status);

    List<Projeto> findByIdResponsavel(Long idResponsavel);
}