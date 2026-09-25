package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fablab.producao.dto.NivelAlteradoEvent;
import com.fablab.producao.consumer.NivelAlteradoListener;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NivelAlteradoListenerTest {

    private final NivelAlteradoListener listener = new NivelAlteradoListener();

    @Test
    void processaAlteracaoDeNivel() {
        assertDoesNotThrow(() -> listener.onNivelAlterado(new NivelAlteradoEvent(
                5L, "ESTAGIARIO", "BOLSISTA", Instant.now())));
    }
}