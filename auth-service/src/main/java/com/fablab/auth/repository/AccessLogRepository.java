package com.fablab.auth.repository;

import com.fablab.auth.entity.AccessLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code access_log}.
 */
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Optional<AccessLog> findTopByUuidRfidOrderByTimestampDesc(String uuidRfid);
}