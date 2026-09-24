package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.auth.entity.AccessLog;
import com.fablab.auth.entity.AccessLogType;
import com.fablab.auth.repository.AccessLogRepository;
import com.fablab.auth.service.AccessLogService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessLogServiceTest {

    @Mock
    private AccessLogRepository repository;

    @InjectMocks
    private AccessLogService service;

    @Test
    void resolvesToEntradaWhenNoPreviousLog() {
        when(repository.findTopByUuidRfidOrderByTimestampDesc("CARD-001"))
                .thenReturn(Optional.empty());

        assertThat(service.resolveType("CARD-001")).isEqualTo(AccessLogType.ENTRADA);
    }

    @Test
    void resolvesToSaidaWhenLastWasEntrada() {
        when(repository.findTopByUuidRfidOrderByTimestampDesc("CARD-001"))
                .thenReturn(Optional.of(TestLog.entrada("CARD-001")));

        assertThat(service.resolveType("CARD-001")).isEqualTo(AccessLogType.SAIDA);
    }

    @Test
    void resolvesToEntradaWhenLastWasSaida() {
        when(repository.findTopByUuidRfidOrderByTimestampDesc("CARD-001"))
                .thenReturn(Optional.of(TestLog.saida("CARD-001")));

        assertThat(service.resolveType("CARD-001")).isEqualTo(AccessLogType.ENTRADA);
    }

    @Test
    void recordsAccessLogWithTimestamp() {
        AccessLog saved = new AccessLog();
        when(repository.save(any(AccessLog.class))).thenReturn(saved);

        AccessLog result = service.record(7L, "CARD-001", AccessLogType.ENTRADA);

        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(repository).save(captor.capture());
        AccessLog captured = captor.getValue();
        assertThat(captured.getIdUser()).isEqualTo(7L);
        assertThat(captured.getUuidRfid()).isEqualTo("CARD-001");
        assertThat(captured.getType()).isEqualTo(AccessLogType.ENTRADA);
        assertThat(captured.getTimestamp()).isNotNull();
        assertThat(result).isSameAs(saved);
    }

    private static final class TestLog {
        private TestLog() {
        }

        static AccessLog entrada(String uuid) {
            return log(uuid, AccessLogType.ENTRADA);
        }

        static AccessLog saida(String uuid) {
            return log(uuid, AccessLogType.SAIDA);
        }

        private static AccessLog log(String uuid, AccessLogType type) {
            AccessLog log = new AccessLog();
            log.setUuidRfid(uuid);
            log.setType(type);
            log.setTimestamp(Instant.now());
            return log;
        }
    }
}