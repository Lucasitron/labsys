package com.fablab.vendas.repository;

import com.fablab.vendas.entity.TagCliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de tags de clientes.
 */
public interface TagClienteRepository extends JpaRepository<TagCliente, Long> {

    Optional<TagCliente> findByNomeIgnoreCase(String nome);
}