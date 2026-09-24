package com.fablab.producao.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fablab.producao.dto.Parametro5SRequest;
import com.fablab.producao.entity.Parametro5S;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.Parametro5SRepository;
import com.fablab.producao.service.Parametro5SService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Parametro5SServiceTest {

    @Mock
    private Parametro5SRepository parametroRepository;

    @InjectMocks
    private Parametro5SService service;

    private Parametro5S parametro(Long id, String chave, String valor) {
        Parametro5S p = new Parametro5S();
        p.setIdParametro(id);
        p.setChave(chave);
        p.setValor(valor);
        return p;
    }

    @Test
    void listarTodos() {
        when(parametroRepository.findAll()).thenReturn(List.of(parametro(1L, "rotacaoDias", "7")));
        assertEquals(1, service.listar().size());
    }

    @Test
    void buscar() {
        when(parametroRepository.findById(1L)).thenReturn(Optional.of(parametro(1L, "rotacaoDias", "7")));
        assertEquals("rotacaoDias", service.buscar(1L).chave());
    }

    @Test
    void buscarInexistenteLancaExcecao() {
        when(parametroRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscar(9L));
    }

    @Test
    void atualizarValorEDescricao() {
        when(parametroRepository.findById(1L)).thenReturn(Optional.of(parametro(1L, "rotacaoDias", "7")));

        var response = service.atualizar(1L, new Parametro5SRequest("14", "nova descrição"));

        assertEquals("14", response.valor());
        assertEquals("nova descrição", response.descricao());
    }

    @Test
    void obterInteiroLeValorConfigurado() {
        when(parametroRepository.findByChave("rotacaoDias")).thenReturn(Optional.of(parametro(1L, "rotacaoDias", "14")));
        assertEquals(14, service.obterInteiro("rotacaoDias", 7));
    }

    @Test
    void obterInteiroUsaPadraoQuandoAusente() {
        when(parametroRepository.findByChave("x")).thenReturn(Optional.empty());
        assertEquals(7, service.obterInteiro("x", 7));
    }

    @Test
    void obterInteiroUsaPadraoQuandoInvalido() {
        when(parametroRepository.findByChave("x")).thenReturn(Optional.of(parametro(1L, "x", "abc")));
        assertEquals(7, service.obterInteiro("x", 7));
    }
}