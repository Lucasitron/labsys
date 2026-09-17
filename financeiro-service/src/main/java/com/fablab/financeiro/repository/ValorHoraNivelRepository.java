package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.ValorHoraNivel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de valor de hora por nível de acesso.
 */
public interface ValorHoraNivelRepository extends JpaRepository<ValorHoraNivel, Long> {

    Optional<ValorHoraNivel> findFirstByNivelAcessoOrderByDataVigenciaDesc(Integer nivelAcesso);

    List<ValorHoraNivel> findAllByOrderByNivelAcessoAscDataVigenciaDesc();
}