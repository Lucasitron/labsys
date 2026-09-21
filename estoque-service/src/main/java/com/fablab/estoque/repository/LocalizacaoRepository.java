package com.fablab.estoque.repository;

import com.fablab.estoque.entity.Localizacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de localizações físicas.
 */
public interface LocalizacaoRepository extends JpaRepository<Localizacao, Long> {

    /** Listagem ordenada pelo endereçamento físico (armário, prateleira, caixa). */
    List<Localizacao> findAllByOrderByArmarioAscPrateleiraAscCaixaAsc();
}