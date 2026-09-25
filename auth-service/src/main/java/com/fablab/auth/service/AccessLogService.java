package com.fablab.auth.service;

import com.fablab.auth.entity.AccessLog;
import com.fablab.auth.entity.AccessLogType;
import com.fablab.auth.repository.AccessLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Registro do acesso físico (ponto eletrônico) em {@code access_log}.
 */
@Service
public class AccessLogService {

    private final AccessLogRepository repository;

    public AccessLogService(AccessLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Alterna entre ENTRADA e SAIDA com base no último registro do cartão.
     * Se não houver registro anterior, considera ENTRADA.
     */
    public AccessLogType resolveType(String uuidRfid) {
        return repository.findTopByUuidRfidOrderByTimestampDesc(uuidRfid)
                .map(previous -> previous.getType() == AccessLogType.ENTRADA
                        ? AccessLogType.SAIDA
                        : AccessLogType.ENTRADA)
                .orElse(AccessLogType.ENTRADA);
    }

    /**
     * Persiste um registro de acesso.
     */
    public AccessLog record(Long idUser, String uuidRfid, AccessLogType type) {
        AccessLog log = new AccessLog();
        log.setIdUser(idUser);
        log.setUuidRfid(uuidRfid);
        log.setType(type);
        log.setTimestamp(Instant.now());
        return repository.save(log);
    }
}