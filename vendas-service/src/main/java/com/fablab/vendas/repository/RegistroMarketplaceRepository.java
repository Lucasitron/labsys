package com.fablab.vendas.repository;

import com.fablab.vendas.entity.RegistroMarketplace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de registros manuais de vendas em marketplace.
 */
public interface RegistroMarketplaceRepository extends JpaRepository<RegistroMarketplace, Long> {

    List<RegistroMarketplace> findByEncomenda_Id(Long idEncomenda);
}