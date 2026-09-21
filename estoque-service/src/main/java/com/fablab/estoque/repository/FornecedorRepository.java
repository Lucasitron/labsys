package com.fablab.estoque.repository;

import com.fablab.estoque.entity.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de fornecedores.
 */
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
}