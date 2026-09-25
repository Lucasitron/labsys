package com.fablab.financeiro.repository;

import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.TipoCategoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, Long> {

    List<CategoriaFinanceira> findByTipo(TipoCategoria tipo);
}
