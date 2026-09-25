package com.fablab.producao.repository;

import com.fablab.producao.entity.Parametro5S;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Parametro5SRepository extends JpaRepository<Parametro5S, Long> {

    Optional<Parametro5S> findByChave(String chave);
}