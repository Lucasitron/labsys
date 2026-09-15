package com.fablab.vendas.repository;

import com.fablab.vendas.entity.InteracaoCliente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de interações com clientes.
 */
public interface InteracaoClienteRepository extends JpaRepository<InteracaoCliente, Long> {

    List<InteracaoCliente> findByCliente_IdOrderByDataInteracaoDesc(Long idCliente);
}