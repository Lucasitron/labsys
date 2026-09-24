package com.fablab.vendas.repository;

import com.fablab.vendas.entity.RegistroMarketplace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório de registros manuais de marketplace. */
public interface RegistroMarketplaceRepository extends JpaRepository<RegistroMarketplace, Long> {

    List<RegistroMarketplace> findByIdEncomenda(Long idEncomenda);

    List<RegistroMarketplace> findByPlataforma(String plataforma);

    Optional<RegistroMarketplace> findByPlataformaAndCodigoExterno(String plataforma, String codigoExterno);

    boolean existsByPlataformaAndCodigoExterno(String plataforma, String codigoExterno);
}
