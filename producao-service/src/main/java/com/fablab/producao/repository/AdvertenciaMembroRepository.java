package com.fablab.producao.repository;

import com.fablab.producao.entity.AdvertenciaMembro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertenciaMembroRepository extends JpaRepository<AdvertenciaMembro, Long> {

    List<AdvertenciaMembro> findByIdFuncionarioOrderByDataDesc(Long idFuncionario);

    long countByIdFuncionario(Long idFuncionario);
}