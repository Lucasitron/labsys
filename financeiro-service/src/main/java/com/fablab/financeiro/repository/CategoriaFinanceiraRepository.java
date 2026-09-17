package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.TipoCategoriaFinanceira;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de categorias financeiras.
 */
public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, Long> {

    List<CategoriaFinanceira> findByTipo(TipoCategoriaFinanceira tipo);

    boolean existsByNomeIgnoreCase(String nome);
}