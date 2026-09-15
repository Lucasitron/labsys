package com.fablab.vendas.repository;

import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.TipoPessoa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de clientes.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCpfCnpj(String cpfCnpj);

    List<Cliente> findByTipoPessoa(TipoPessoa tipoPessoa);

    List<Cliente> findByNomeRazaoSocialContainingIgnoreCase(String nome);
}