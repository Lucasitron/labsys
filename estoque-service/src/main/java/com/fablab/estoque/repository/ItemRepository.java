package com.fablab.estoque.repository;

import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Item;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de itens de inventário.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByCategoria(Categoria categoria);

    List<Item> findByLocalizacaoId(Long idLocalizacao);

    /** Itens com estoque baixo: {@code quantidade_atual <= estoque_minimo}. */
    List<Item> findByQuantidadeAtualLessThanEqual(BigDecimal valor);

    /**
     * Busca o item com lock pessimista de escrita para atualização segura do
     * saldo em operações de entrada, saída, consumo e empréstimo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);
}