package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.CompraSolicitadaEvent;
import com.fablab.financeiro.dto.SolicitacaoCompraRequest;
import com.fablab.financeiro.entity.SolicitacaoCompra;
import com.fablab.financeiro.entity.StatusSolicitacaoCompra;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.SolicitacaoCompraRepository;
import com.fablab.financeiro.service.FinanceiroEventPublisher;
import com.fablab.financeiro.service.SolicitacaoCompraService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolicitacaoCompraServiceTest {

    @Mock
    private SolicitacaoCompraRepository solicitacaoRepository;
    @Mock
    private FinanceiroEventPublisher eventPublisher;

    @InjectMocks
    private SolicitacaoCompraService service;

    private SolicitacaoCompra solicitacao(StatusSolicitacaoCompra status) {
        SolicitacaoCompra s = new SolicitacaoCompra();
        s.setIdSolicitacao(1L);
        s.setIdItemEstoque(1L);
        s.setQuantidade(BigDecimal.TEN);
        s.setValorEstimado(BigDecimal.valueOf(100));
        s.setStatus(status);
        s.setDataSolicitacao(LocalDate.now());
        return s;
    }

    @Test
    void criarRegistraEPublica() {
        when(solicitacaoRepository.save(any(SolicitacaoCompra.class))).thenAnswer(inv -> {
            SolicitacaoCompra s = inv.getArgument(0);
            s.setIdSolicitacao(1L);
            return s;
        });

        var response = service.criar(new SolicitacaoCompraRequest(1L, BigDecimal.TEN, BigDecimal.valueOf(100)));

        assertEquals(StatusSolicitacaoCompra.REGISTRADA, response.status());
        verify(eventPublisher).publishCompraSolicitada(any(CompraSolicitadaEvent.class));
    }

    @Test
    void concluirAlteraStatus() {
        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao(StatusSolicitacaoCompra.REGISTRADA)));
        when(solicitacaoRepository.save(any(SolicitacaoCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.concluir(1L);

        assertEquals(StatusSolicitacaoCompra.CONCLUIDA, response.status());
    }

    @Test
    void concluirRejeitaJaConcluida() {
        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao(StatusSolicitacaoCompra.CONCLUIDA)));
        assertThrows(IllegalArgumentException.class, () -> service.concluir(1L));
    }

    @Test
    void concluirLancaErroQuandoInexistente() {
        when(solicitacaoRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.concluir(1L));
    }
}