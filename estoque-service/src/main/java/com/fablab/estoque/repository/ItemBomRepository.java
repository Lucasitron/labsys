package com.fablab.estoque.repository;

import com.fablab.estoque.entity.ItemBom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de itens de BOM.
 */
public interface ItemBomRepository extends JpaRepository<ItemBom, Long> {

    /** Itens de uma Lista de Materiais (BOM). */
    List<ItemBom> findByBomId(Long idBom);
}