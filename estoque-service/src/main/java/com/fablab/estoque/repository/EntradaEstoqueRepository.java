package com.fablab.estoque.repository;

import com.fablab.estoque.entity.EntradaEstoque;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de entradas de estoque.
 */
public interface EntradaEstoqueRepository extends JpaRepository<EntradaEstoque, Long> {

    /** Entradas de um item (histórico de compras). */
    List<EntradaEstoque> findByItemId(Long idItem);
}