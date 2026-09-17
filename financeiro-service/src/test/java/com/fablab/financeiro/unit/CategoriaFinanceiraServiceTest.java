package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.CategoriaFinanceiraRequest;
import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.TipoCategoriaFinanceira;
import com.fablab.financeiro.repository.CategoriaFinanceiraRepository;
import com.fablab.financeiro.service.CategoriaFinanceiraService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoriaFinanceiraServiceTest {

    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;

    @InjectMocks
    private CategoriaFinanceiraService service;

    private CategoriaFinanceira categoria(Long id, TipoCategoriaFinanceira tipo) {
        CategoriaFinanceira c = new CategoriaFinanceira();
        c.setIdCategoria(id);
        c.setNome("Material");
        c.setTipo(tipo);
        return c;
    }

    @Test
    void criarPersisteCategoria() {
        when(categoriaRepository.existsByNomeIgnoreCase("Material")).thenReturn(false);
        when(categoriaRepository.save(any(CategoriaFinanceira.class))).thenAnswer(inv -> {
            CategoriaFinanceira c = inv.getArgument(0);
            c.setIdCategoria(1L);
            return c;
        });

        var response = service.criar(new CategoriaFinanceiraRequest(
                "Material", TipoCategoriaFinanceira.DESPESA, "Insumos"));

        assertEquals(1L, response.idCategoria());
        assertEquals(TipoCategoriaFinanceira.DESPESA, response.tipo());
    }

    @Test
    void criarRejeitaNomeDuplicado() {
        when(categoriaRepository.existsByNomeIgnoreCase("Material")).thenReturn(true);
        var request = new CategoriaFinanceiraRequest("Material", TipoCategoriaFinanceira.DESPESA, null);
        assertThrows(IllegalArgumentException.class, () -> service.criar(request));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void listarPorTipo() {
        when(categoriaRepository.findByTipo(TipoCategoriaFinanceira.RECEITA))
                .thenReturn(List.of(categoria(1L, TipoCategoriaFinanceira.RECEITA)));

        var lista = service.listar(TipoCategoriaFinanceira.RECEITA);

        assertEquals(1, lista.size());
        verify(categoriaRepository, never()).findAll();
    }

    @Test
    void listarSemTipoRetornaTodas() {
        when(categoriaRepository.findAll())
                .thenReturn(List.of(categoria(1L, TipoCategoriaFinanceira.RECEITA),
                        categoria(2L, TipoCategoriaFinanceira.DESPESA)));

        var lista = service.listar(null);

        assertEquals(2, lista.size());
    }
}