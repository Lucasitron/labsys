package com.fablab.financeiro.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.CompraDtos.CompraRequest;
import com.fablab.financeiro.dto.DoacaoDtos.DoacaoRequest;
import com.fablab.financeiro.entity.DoacaoRecurso;
import com.fablab.financeiro.entity.SolicitacaoCompra;
import com.fablab.financeiro.entity.StatusCompra;
import com.fablab.financeiro.entity.TipoDoacao;
import com.fablab.financeiro.repository.CategoriaFinanceiraRepository;
import com.fablab.financeiro.repository.DoacaoRecursoRepository;
import com.fablab.financeiro.repository.SolicitacaoCompraRepository;
import com.fablab.financeiro.service.CategoriaFinanceiraService;
import com.fablab.financeiro.service.DoacaoRecursoService;
import com.fablab.financeiro.service.FinanceiroEventPublisher;
import com.fablab.financeiro.service.SolicitacaoCompraService;
import com.fablab.financeiro.dto.CategoriaDtos.CategoriaRequest;
import com.fablab.financeiro.entity.TipoCategoria;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Regras de categorias, doações/recursos e compras (fluxo informativo). */
@ExtendWith(MockitoExtension.class)
class CategoriaDoacaoCompraTest {

    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;
    @Mock
    private DoacaoRecursoRepository doacaoRepository;
    @Mock
    private SolicitacaoCompraRepository compraRepository;
    @Mock
    private FinanceiroEventPublisher publisher;

    @Test
    void categoriaCriadaComTipo() {
        when(categoriaRepository.save(any())).thenAnswer(i -> {
            com.fablab.financeiro.entity.CategoriaFinanceira c = i.getArgument(0);
            c.setId(1L);
            return c;
        });
        var service = new CategoriaFinanceiraService(categoriaRepository);
        var resposta = service.criar(new CategoriaRequest("Vendas", TipoCategoria.RECEITA, "Receitas de vendas"));
        assertThat(resposta.tipo()).isEqualTo(TipoCategoria.RECEITA);
    }

    @Test
    void doacaoRegistradaComProjetoOpcional() {
        when(doacaoRepository.save(any())).thenAnswer(i -> {
            DoacaoRecurso d = i.getArgument(0);
            d.setId(1L);
            return d;
        });
        var service = new DoacaoRecursoService(doacaoRepository);
        var resposta = service.registrar(new DoacaoRequest(TipoDoacao.DOACAO,
                "Grupo Mãos que Criam", new BigDecimal("1500.00"), LocalDate.now(), null));
        assertThat(resposta.tipo()).isEqualTo(TipoDoacao.DOACAO);
        assertThat(resposta.idProjetoAssociado()).isNull();
    }

    @Test
    void compraRegistradaPublicaEventoEConclui() {
        when(compraRepository.save(any())).thenAnswer(i -> {
            SolicitacaoCompra s = i.getArgument(0);
            if (s.getId() == null) {
                s.setId(1L);
            }
            return s;
        });
        var service = new SolicitacaoCompraService(compraRepository, publisher);
        var criada = service.registrar(new CompraRequest(10, new BigDecimal("5.00"), new BigDecimal("248.90")));

        assertThat(criada.status()).isEqualTo(StatusCompra.REGISTRADA);
        verify(publisher).publishCompraSolicitada(any());

        SolicitacaoCompra existente = new SolicitacaoCompra();
        existente.setId(1L);
        existente.setStatus(StatusCompra.VISUALIZADA);
        when(compraRepository.findById(1L)).thenReturn(Optional.of(existente));
        var concluida = service.concluir(1L);
        assertThat(concluida.status()).isEqualTo(StatusCompra.CONCLUIDA);
    }

    @Test
    void concluirCompraJaConcluidaRejeita() {
        SolicitacaoCompra existente = new SolicitacaoCompra();
        existente.setId(2L);
        existente.setStatus(StatusCompra.CONCLUIDA);
        when(compraRepository.findById(2L)).thenReturn(Optional.of(existente));
        var service = new SolicitacaoCompraService(compraRepository, publisher);
        assertThatThrownBy(() -> service.concluir(2L)).isInstanceOf(IllegalArgumentException.class);
    }
}
