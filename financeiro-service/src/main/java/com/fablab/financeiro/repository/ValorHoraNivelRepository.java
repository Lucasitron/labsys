package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.ValorHoraNivel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValorHoraNivelRepository extends JpaRepository<ValorHoraNivel, Long> {

    /** Vigente por nível: registro com maior data de vigência. */
    Optional<ValorHoraNivel> findFirstByNivelAcessoOrderByDataVigenciaDesc(Integer nivelAcesso);

    List<ValorHoraNivel> findByNivelAcessoOrderByDataVigenciaDesc(Integer nivelAcesso);
}
