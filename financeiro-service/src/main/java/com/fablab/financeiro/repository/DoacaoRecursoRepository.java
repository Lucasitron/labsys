package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.DoacaoRecurso;
import com.fablab.financeiro.entity.TipoDoacao;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoacaoRecursoRepository extends JpaRepository<DoacaoRecurso, Long> {

    List<DoacaoRecurso> findByTipo(TipoDoacao tipo);

    List<DoacaoRecurso> findByDataRecebimentoBetween(LocalDate inicio, LocalDate fim);

    List<DoacaoRecurso> findByTipoAndDataRecebimentoBetween(TipoDoacao tipo, LocalDate inicio, LocalDate fim);
}
