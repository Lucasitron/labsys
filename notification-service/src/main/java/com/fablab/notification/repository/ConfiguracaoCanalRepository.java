package com.fablab.notification.repository;

import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.ConfiguracaoCanal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório das configurações de canal. */
public interface ConfiguracaoCanalRepository extends JpaRepository<ConfiguracaoCanal, Long> {

    Optional<ConfiguracaoCanal> findByCanal(CanalNotificacao canal);
}
