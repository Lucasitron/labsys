package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fablab.auth.entity.AccessLog;
import com.fablab.auth.entity.AccessLogType;
import com.fablab.auth.repository.AccessLogRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class AccessLogRepositoryTest {

    @Autowired
    private AccessLogRepository repository;

    @Test
    void findsMostRecentLogForUuid() {
        repository.save(log("CARD-001", AccessLogType.ENTRADA, Instant.parse("2026-01-01T08:00:00Z")));
        repository.save(log("CARD-001", AccessLogType.SAIDA, Instant.parse("2026-01-01T18:00:00Z")));
        repository.save(log("CARD-002", AccessLogType.ENTRADA, Instant.parse("2026-01-01T09:00:00Z")));

        assertThat(repository.findTopByUuidRfidOrderByTimestampDesc("CARD-001"))
                .get()
                .extracting(AccessLog::getType)
                .isEqualTo(AccessLogType.SAIDA);
    }

    @Test
    void returnsEmptyWhenNoLogExists() {
        assertThat(repository.findTopByUuidRfidOrderByTimestampDesc("CARD-NONE")).isEmpty();
    }

    private AccessLog log(String uuid, AccessLogType type, Instant timestamp) {
        AccessLog log = new AccessLog();
        log.setUuidRfid(uuid);
        log.setType(type);
        log.setTimestamp(timestamp);
        return repository.save(log);
    }
}