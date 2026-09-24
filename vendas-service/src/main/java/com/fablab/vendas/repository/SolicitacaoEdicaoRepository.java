package com.fablab.vendas.repository;

import com.fablab.vendas.entity.SolicitacaoEdicao;
import com.fablab.vendas.entity.StatusSolicitacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório de solicitações de edição. */
public interface SolicitacaoEdicaoRepository extends JpaRepository<SolicitacaoEdicao, Long> {

    List<SolicitacaoEdicao> findByStatus(StatusSolicitacao status);

    long countByStatus(StatusSolicitacao status);
}
