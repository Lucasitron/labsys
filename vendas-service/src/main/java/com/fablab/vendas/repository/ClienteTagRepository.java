package com.fablab.vendas.repository;

import com.fablab.vendas.entity.ClienteTag;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório do relacionamento cliente-tag.
 */
public interface ClienteTagRepository extends JpaRepository<ClienteTag, Long> {

    boolean existsByCliente_IdAndTag_Id(Long idCliente, Long idTag);
}