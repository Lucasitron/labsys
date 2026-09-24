package com.fablab.producao.repository;

import com.fablab.producao.entity.SetorSinalizacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorSinalizacaoRepository extends JpaRepository<SetorSinalizacao, Long> {

    List<SetorSinalizacao> findBySetor_IdSetor(Long idSetor);
}