package com.fablab.vendas.repository;

import com.fablab.vendas.entity.ClienteTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do vínculo N:N cliente↔tag. */
public interface ClienteTagRepository extends JpaRepository<ClienteTag, Long> {

    List<ClienteTag> findByIdCliente(Long idCliente);

    List<ClienteTag> findByIdTag(Long idTag);

    boolean existsByIdClienteAndIdTag(Long idCliente, Long idTag);

    void deleteByIdClienteAndIdTag(Long idCliente, Long idTag);

    long countByIdTag(Long idTag);
}
