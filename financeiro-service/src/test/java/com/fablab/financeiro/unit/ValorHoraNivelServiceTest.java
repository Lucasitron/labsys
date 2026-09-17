package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.ValorHoraNivelRequest;
import com.fablab.financeiro.entity.ValorHoraNivel;
import com.fablab.financeiro.repository.ValorHoraNivelRepository;
import com.fablab.financeiro.service.ValorHoraNivelService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValorHoraNivelServiceTest {

    @Mock
    private ValorHoraNivelRepository valorHoraRepository;

    @InjectMocks
    private ValorHoraNivelService service;

    private ValorHoraNivel valorHora(int nivel, double valor) {
        ValorHoraNivel v = new ValorHoraNivel();
        v.setIdValorHora(1L);
        v.setNivelAcesso(nivel);
        v.setValorHora(BigDecimal.valueOf(valor));
        v.setDataVigencia(LocalDate.now());
        return v;
    }

    @Test
    void definirPersiste() {
        when(valorHoraRepository.save(any(ValorHoraNivel.class))).thenAnswer(inv -> {
            ValorHoraNivel v = inv.getArgument(0);
            v.setIdValorHora(1L);
            return v;
        });

        var response = service.definir(new ValorHoraNivelRequest(
                1, BigDecimal.valueOf(50), LocalDate.now()));

        assertEquals(1, response.nivelAcesso());
    }

    @Test
    void listarVigentesRetornaApenasNiveisConfigurados() {
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(1))
                .thenReturn(Optional.of(valorHora(1, 50)));
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(0))
                .thenReturn(Optional.empty());
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(2))
                .thenReturn(Optional.empty());
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(3))
                .thenReturn(Optional.empty());

        var lista = service.listarVigentes();

        assertEquals(1, lista.size());
        assertEquals(1, lista.get(0).nivelAcesso());
    }

    @Test
    void obterValorHoraPorNivelDelega() {
        when(valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(2))
                .thenReturn(Optional.of(valorHora(2, 30)));

        assertTrue(service.obterValorHoraPorNivel(2).isPresent());
    }
}