package com.fablab.vendas.repository;

import com.fablab.vendas.entity.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório de clientes com filtros por nome, tipo e tag. */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCpfCnpj(String cpfCnpj);

    boolean existsByCpfCnpj(String cpfCnpj);

    List<Cliente> findByTipoPessoa(com.fablab.vendas.entity.TipoPessoa tipoPessoa);

    @Query("SELECT c FROM Cliente c WHERE LOWER(c.nomeRazaoSocial) LIKE LOWER(CONCAT('%', :termo, '%'))"
            + " OR LOWER(c.email) LIKE LOWER(CONCAT('%', :termo, '%'))"
            + " OR c.cpfCnpj LIKE CONCAT('%', :termo, '%')")
    Page<Cliente> buscar(@Param("termo") String termo, Pageable pageable);

    @Query("SELECT c FROM Cliente c JOIN ClienteTag ct ON ct.idCliente = c.id"
            + " WHERE ct.idTag IN :tagIds GROUP BY c HAVING COUNT(DISTINCT ct.idTag) = :qtd")
    List<Cliente> buscarPorTags(@Param("tagIds") List<Long> tagIds, @Param("qtd") long qtd);

    long countByTipoPessoa(com.fablab.vendas.entity.TipoPessoa tipoPessoa);
}
