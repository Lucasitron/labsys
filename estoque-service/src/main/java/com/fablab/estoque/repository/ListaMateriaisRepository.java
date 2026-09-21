package com.fablab.estoque.repository;

import com.fablab.estoque.entity.ListaMateriais;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de Listas de Materiais (BOM).
 */
public interface ListaMateriaisRepository extends JpaRepository<ListaMateriais, Long> {

    /** BOMs de um produto/serviço. */
    List<ListaMateriais> findByIdProdutoServico(Long idProdutoServico);

    /** Versão específica da BOM de um produto/serviço. */
    Optional<ListaMateriais> findByIdProdutoServicoAndVersao(Long idProdutoServico, Integer versao);

    /** Carrega a BOM com seus itens em uma única consulta. */
    @EntityGraph(attributePaths = {"itens", "itens.item"})
    @Query("select b from ListaMateriais b where b.id = :id")
    Optional<ListaMateriais> findWithItensById(@Param("id") Long id);
}