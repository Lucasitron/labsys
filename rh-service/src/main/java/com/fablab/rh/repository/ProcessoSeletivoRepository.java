package com.fablab.rh.repository;

import com.fablab.rh.entity.ProcessoSeletivo;
import com.fablab.rh.entity.StatusProcesso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code processo_seletivo}.
 */
public interface ProcessoSeletivoRepository extends JpaRepository<ProcessoSeletivo, Long> {

    Optional<ProcessoSeletivo> findByCandidatoId(Long pessoaId);

    boolean existsByCandidatoId(Long pessoaId);

    List<ProcessoSeletivo> findByStatusProcesso(StatusProcesso status);

    List<ProcessoSeletivo> findByGrupoId(Long grupoId);

    List<ProcessoSeletivo> findByGrupoIsNull();

    long countByStatusProcesso(StatusProcesso status);
}