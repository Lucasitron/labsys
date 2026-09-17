package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.DoacaoRecursoRequest;
import com.fablab.financeiro.entity.DoacaoRecurso;
import com.fablab.financeiro.entity.TipoDoacaoRecurso;
import com.fablab.financeiro.repository.DoacaoRecursoRepository;
import com.fablab.financeiro.service.DoacaoRecursoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoacaoRecursoServiceTest {

    @Mock
    private DoacaoRecursoRepository doacaoRepository;

    @InjectMocks
    private DoacaoRecursoService service;

    private DoacaoRecurso doacao(Long id, TipoDoacaoRecurso tipo) {
        DoacaoRecurso d = new DoacaoRecurso();
        d.setIdDoacao(id);
        d.setTipo(tipo);
        d.setOrigem("Empresa X");
        d.setValor(BigDecimal.TEN);
        d.setDataRecebimento(LocalDate.now());
        return d;
    }

    @Test
    void criarPersiste() {
        when(doacaoRepository.save(any(DoacaoRecurso.class))).thenAnswer(inv -> {
            DoacaoRecurso d = inv.getArgument(0);
            d.setIdDoacao(1L);
            return d;
        });

        var response = service.criar(new DoacaoRecursoRequest(
                TipoDoacaoRecurso.DOACAO, "Empresa X", BigDecimal.TEN, LocalDate.now(), null));

        assertEquals(1L, response.idDoacao());
    }

    @Test
    void listarPorPeriodoETipo() {
        when(doacaoRepository.findByDataRecebimentoBetween(any(), any()))
                .thenReturn(List.of(doacao(1L, TipoDoacaoRecurso.DOACAO), doacao(2L, TipoDoacaoRecurso.PROJETO)));

        var lista = service.listar(TipoDoacaoRecurso.PROJETO, LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(1, lista.size());
        assertEquals(TipoDoacaoRecurso.PROJETO, lista.get(0).tipo());
    }

    @Test
    void listarSemPeriodoRetornaTodas() {
        when(doacaoRepository.findAll())
                .thenReturn(List.of(doacao(1L, TipoDoacaoRecurso.DOACAO)));

        var lista = service.listar(null, null, null);

        assertEquals(1, lista.size());
    }
}