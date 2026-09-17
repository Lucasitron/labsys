package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.ParametroOverheadRequest;
import com.fablab.financeiro.entity.ParametroOverhead;
import com.fablab.financeiro.repository.ParametroOverheadRepository;
import com.fablab.financeiro.service.ParametroOverheadService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParametroOverheadServiceTest {

    @Mock
    private ParametroOverheadRepository parametroRepository;

    @InjectMocks
    private ParametroOverheadService service;

    @Test
    void definirPersiste() {
        when(parametroRepository.save(any(ParametroOverhead.class))).thenAnswer(inv -> {
            ParametroOverhead p = inv.getArgument(0);
            p.setIdParametro(1L);
            return p;
        });

        var response = service.definir(new ParametroOverheadRequest(BigDecimal.valueOf(5), LocalDate.now()));

        assertEquals(1L, response.idParametro());
    }

    @Test
    void taxaVigenteRetornaOptionalVazioQuandoNaoConfigurado() {
        when(parametroRepository.findTopByOrderByDataVigenciaDesc()).thenReturn(Optional.empty());
        assertTrue(service.taxaVigente().isEmpty());
    }
}