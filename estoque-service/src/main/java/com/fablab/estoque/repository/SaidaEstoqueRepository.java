package com.fablab.estoque.repository;

import com.fablab.estoque.entity.SaidaEstoque;
import com.fablab.estoque.entity.TipoSaida;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de saídas de estoque.
 */
public interface SaidaEstoqueRepository extends JpaRepository<SaidaEstoque, Long> {

    /** Saídas de um item (histórico de movimentações). */
    List<SaidaEstoque> findByItemId(Long idItem);

    /** Saídas vinculadas a uma referência externa (ex.: id encomenda/produção). */
    List<SaidaEstoque> findByIdReferencia(Long idReferencia);

    /** Indica se já há baixa de consumo para a referência (idempotência do evento de produção). */
    boolean existsByTipoSaidaAndIdReferencia(TipoSaida tipoSaida, Long idReferencia);
}