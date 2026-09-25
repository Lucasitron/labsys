package com.fablab.rh.repository;

import com.fablab.rh.entity.GrupoProcessoSeletivo;
import com.fablab.rh.entity.StatusProcesso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code grupo_processo_seletivo}.
 */
public interface GrupoProcessoSeletivoRepository extends JpaRepository<GrupoProcessoSeletivo, Long> {

    List<GrupoProcessoSeletivo> findByEtapa(StatusProcesso etapa);
}
